# SRC [![Build status](https://badge.buildkite.com/1083ac505d1e05cba8a2687e279e062eff81a30d23a781f5d5.svg)](https://buildkite.com/nerm/pscfk-src)

## Stateful rebalance client

- Client used to show how to handle state on startup / shutdown when Kafka Streams is not an option
- When copartition is required, must have consistent partition reassignment to avoid rebuilding state.
- free consumer for one partition topic
- testcontainers

```
players topic : 5p
scores topic : 5p
products topic : 1p
```
