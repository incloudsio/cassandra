/*
 * Elassandra: bridge package-private SchemaKeyspace mutation builders for org.elassandra.cluster.SchemaManager.
 */
package org.apache.cassandra.schema;

import org.apache.cassandra.db.Mutation;

/**
 * Delegates to {@link SchemaKeyspace} static helpers that are otherwise package-private to {@code org.apache.cassandra.schema}.
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
