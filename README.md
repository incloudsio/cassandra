# Apache Cassandra fork for [Elassandra](https://elassandra.org/)

This repository is the **Cassandra** side of [Elassandra](https://github.com/incloudsio/elassandra): Cassandra with changes so each node can embed the search engine (historically Elasticsearch; modernization targets [OpenSearch](https://opensearch.org/) 1.3.x).

Upstream work tracks **Apache Cassandra**. Elassandra-specific commits build on that line for use as the [`server/cassandra` git submodule](https://github.com/incloudsio/elassandra/tree/master/server/cassandra) in the Elassandra project.

## Branches

| Branch | Purpose |
|--------|---------|
| **`cassandra-3.11.9-elassandra`** | Current production line for Elassandra 6.8.x (Cassandra **3.11.9** family). |
| **`cassandra-4.0.x-elassandra`** | Port of Elassandra-specific changes toward **Apache Cassandra 4.0.x** (work in progress). |

Default branch on GitHub is usually **`cassandra-3.11.9-elassandra`** (what Elassandra builds against today).

## Build

See [Apache Cassandra](https://github.com/apache/cassandra) for general build instructions (Ant, JDK). Published artifact **`groupId`** / **`artifactId`** pairs are defined in `build.xml`; Elassandra resolves them via Gradle (see [CONTRIBUTING](https://github.com/incloudsio/elassandra/blob/master/CONTRIBUTING.md)).

## Contributing

Cassandra changes that exist only for Elassandra should be discussed in the [Elassandra](https://github.com/incloudsio/elassandra) repo; fork-specific patches are tracked in Elassandra’s `docs` and `scripts` (e.g. `export-cassandra-elassandra-patches.sh`).

## License

Apache License 2.0. This fork includes history and copyright from the Apache Cassandra project and prior maintainers; see `NOTICE.txt` and license headers in-tree.
