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
package org.apache.cassandra.schema;

import org.apache.cassandra.db.Mutation;

/**
 * Elassandra: exposes {@link SchemaKeyspace} mutation helpers to {@code org.elassandra.cluster.SchemaManager}.
 * Delegates to package-private static methods on {@link SchemaKeyspace}.
 */
public final class ElassandraSchemaBridge
{
    private ElassandraSchemaBridge() {}

    public static Mutation.SimpleBuilder makeCreateKeyspaceMutation(KeyspaceMetadata keyspace, long timestamp)
    {
        return SchemaKeyspace.makeCreateKeyspaceMutation(keyspace, timestamp);
    }

    public static Mutation.SimpleBuilder makeCreateKeyspaceMutation(String name, KeyspaceParams params, long timestamp)
    {
        return SchemaKeyspace.makeCreateKeyspaceMutation(name, params, timestamp);
    }

    public static void addTableToSchemaMutation(TableMetadata table, boolean withColumnsAndTriggers, Mutation.SimpleBuilder builder)
    {
        SchemaKeyspace.addTableToSchemaMutation(table, withColumnsAndTriggers, builder);
    }

    public static Mutation.SimpleBuilder makeUpdateTableMutation(KeyspaceMetadata keyspace,
                                                                 TableMetadata oldTable,
                                                                 TableMetadata newTable,
                                                                 long timestamp)
    {
        return SchemaKeyspace.makeUpdateTableMutation(keyspace, oldTable, newTable, timestamp);
    }

    public static Mutation.SimpleBuilder makeDropKeyspaceMutation(KeyspaceMetadata keyspace, long timestamp)
    {
        return SchemaKeyspace.makeDropKeyspaceMutation(keyspace, timestamp);
    }

    public static Mutation.SimpleBuilder makeDropTableMutation(KeyspaceMetadata keyspace, TableMetadata table, long timestamp)
    {
        return SchemaKeyspace.makeDropTableMutation(keyspace, table, timestamp);
    }
}
