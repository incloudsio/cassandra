/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.index.internal;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.concurrent.NamedThreadFactory;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.compaction.CompactionInfo;
import org.apache.cassandra.db.compaction.CompactionInterruptedException;
import org.apache.cassandra.db.compaction.OperationType;
import org.apache.cassandra.dht.Murmur3Partitioner;
import org.apache.cassandra.index.Index;
import org.apache.cassandra.index.SecondaryIndexBuilder;
import org.apache.cassandra.io.sstable.ReducingKeyIterator;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.utils.UUIDGen;

/**
 * Manages building an entire index from column family data. Runs on the compaction manager.
 * When {@code indexThreads &gt; 1} and the partitioner is {@link Murmur3Partitioner}, partitions
 * are distributed across worker threads (Elassandra / CASSANDRA-12837 style).
 */
public class CollatedViewIndexBuilder extends SecondaryIndexBuilder implements AutoCloseable
{
    private static final Logger logger = LoggerFactory.getLogger(CollatedViewIndexBuilder.class);

    public static final int queue_depth = Integer.getInteger("rebuild_index_queue_depth", 128);

    private final ColumnFamilyStore cfs;
    private final Set<Index> indexers;
    private final ReducingKeyIterator iter;
    private final UUID compactionId;
    private final Collection<SSTableReader> sstables;

    private final int indexThreads;
    private final boolean isMultithreaded;
    private final BlockingQueue<DecoratedKey>[] queues;
    private final AtomicBoolean finished;

    public CollatedViewIndexBuilder(ColumnFamilyStore cfs, Set<Index> indexers, ReducingKeyIterator iter, Collection<SSTableReader> sstables)
    {
        this(1, cfs, indexers, iter, sstables);
    }

    @SuppressWarnings("unchecked")
    public CollatedViewIndexBuilder(int indexThreads, ColumnFamilyStore cfs, Set<Index> indexers, ReducingKeyIterator iter, Collection<SSTableReader> sstables)
    {
        this.indexThreads = indexThreads;
        this.cfs = cfs;
        this.indexers = indexers;
        this.iter = iter;
        this.compactionId = UUIDGen.getTimeUUID();
        this.sstables = sstables;

        isMultithreaded = (DatabaseDescriptor.getPartitioner() instanceof Murmur3Partitioner) && (indexThreads > 1);
        if (isMultithreaded)
        {
            finished = new AtomicBoolean(false);
            BlockingQueue<DecoratedKey>[] q = new BlockingQueue[indexThreads];
            queues = q;
            for (int i = 0; i < indexThreads; i++)
                this.queues[i] = new LinkedBlockingQueue<>(queue_depth);
        }
        else
        {
            finished = new AtomicBoolean(false);
            queues = null;
        }
    }

    public CompactionInfo getCompactionInfo()
    {
        return new CompactionInfo(cfs.metadata(),
                OperationType.INDEX_BUILD,
                iter.getBytesRead(),
                iter.getTotalBytes(),
                compactionId,
                sstables);
    }

    public void build()
    {
        ExecutorService indexExecutor = null;
        AtomicLong indexedRows = null;

        long start = System.currentTimeMillis();
        try
        {
            if (isMultithreaded)
            {
                indexedRows = new AtomicLong(0L);
                indexExecutor = Executors.newFixedThreadPool(indexThreads, new NamedThreadFactory("IndexBuilder-" + compactionId));
                for (int i = 0; i < indexThreads; i++)
                    indexExecutor.execute(new IndexBuilderWorker(queues[i], indexedRows));
            }

            int pageSize = cfs.indexManager.calculateIndexingPageSize();
            while (iter.hasNext())
            {
                if (isStopRequested())
                {
                    if (isMultithreaded && indexExecutor != null)
                    {
                        logger.debug("{} stopped.", compactionId);
                        indexExecutor.shutdownNow();
                    }
                    throw new CompactionInterruptedException(getCompactionInfo());
                }
                DecoratedKey key = iter.next();
                if (isMultithreaded)
                {
                    long token = ((Number) key.getToken().getTokenValue()).longValue();
                    try
                    {
                        int q = (int) (((token % indexThreads) + indexThreads) % indexThreads);
                        this.queues[q].put(key);
                    }
                    catch (InterruptedException e)
                    {
                        logger.error("{} failed to put key {}", compactionId, key, e);
                        Thread.currentThread().interrupt();
                    }
                }
                else
                {
                    cfs.indexManager.indexPartition(key, indexers, pageSize);
                }
            }
        }
        finally
        {
            try
            {
                if (isMultithreaded && indexExecutor != null)
                {
                    logger.debug("{} awaiting termination of index rebuild on {}.{}",
                                 compactionId, cfs.metadata().keyspace, cfs.metadata().name);
                    finished.set(true);
                    indexExecutor.shutdown();
                    if (!indexExecutor.awaitTermination(60, TimeUnit.SECONDS))
                        logger.warn("{} index executor did not terminate in 60s", compactionId);
                    long duration = System.currentTimeMillis() - start;
                    logger.debug("{} index rebuild terminated, {} partitions, duration = {}s",
                                 compactionId,
                                 indexedRows != null ? indexedRows.get() : 0,
                                 duration / 1000);
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void close()
    {
        iter.close();
    }

    private final class IndexBuilderWorker implements Runnable
    {
        private final BlockingQueue<DecoratedKey> queue;
        private final AtomicLong indexedRows;

        IndexBuilderWorker(BlockingQueue<DecoratedKey> q, AtomicLong i)
        {
            this.queue = q;
            this.indexedRows = i;
        }

        @Override
        public void run()
        {
            int pageSize = cfs.indexManager.calculateIndexingPageSize();
            int rowCount = 0;
            try
            {
                while (true)
                {
                    DecoratedKey key = queue.poll(5, TimeUnit.SECONDS);
                    if (finished.get() && (key == null))
                        break;

                    if (key != null)
                    {
                        cfs.indexManager.indexPartition(key, indexers, pageSize);
                        rowCount++;
                    }
                }
            }
            catch (Exception e)
            {
                logger.error("error in index builder worker", e);
            }
            indexedRows.addAndGet(rowCount);
            logger.debug("{} partitions indexed.", rowCount);
        }
    }
}
