# Apache Cassandra 4.0.x fork for [Elassandra](https://elassandra.org/)

This branch (**`cassandra-4.0.x-elassandra`**) ports Elassandra-specific changes onto **Apache Cassandra 4.0.x** (pinned to **`base.version`** in `build.xml`, e.g. **4.0.20**).

* **Canonical repo:** [github.com/incloudsio/cassandra](https://github.com/incloudsio/cassandra)
* **Production line today:** branch **`cassandra-3.11.9-elassandra`** (Cassandra 3.11) — what [Elassandra](https://github.com/incloudsio/elassandra) builds by default.
* **Maven `groupId`:** **`io.inclouds.cassandra`** for **`cassandra-all`** (see `build.xml`).

## Status

Porting is **in progress**: many patches from the 3.11 delta are applied; others are deferred (see Elassandra `docs/elassandra/source/developer/cassandra_40_rebase.rst`). Expect to iterate with the OpenSearch 1.3.x integration in the main Elassandra repository.

## Build

Standard Apache Cassandra **Ant** workflow applies. Use **Java 11** for Cassandra 4.0. After `ant jar`, install **`cassandra-all`** to your local Maven repo for Elassandra Gradle, or publish to your registry.

## License

Apache License 2.0 — see Apache Cassandra and in-tree `NOTICE.txt`.
