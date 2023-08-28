#!/usr/bin/env bash

env=$1
site=$2


kafka-avro-console-producer \
  --bootstrap-server localhost:9094 \
  --topic leaderboard.products \
  --property schema.registry.url=https://localhost:8081 \
  --property 'schema.registry.ssl.truststore.location=truststore.jks' \
  --property 'schema.registry.ssl.truststore.password=mystorepassword' \
  --property 'schema.registry.basic.auth.credentials.source=USER_INFO' \
  --property 'schema.registry.basic.auth.user.info=nermin:nermin-secret' \
  --property 'parse.key=true' \
  --property 'value.schema={"type":"record","namespace":"io.nermdev.schemas.avro.leaderboards","name":"Product","fields":[{"name":"id","type":"long"},{"name":"name","type":"string"}]}' \
  --property 'key.serializer=org.apache.kafka.common.serialization.LongSerializer' \
  --producer.config sasl_ssl.properties \
  --property 'key.separator=|' < ./generated_json/games_generated.json



kafka-avro-console-producer \
  --bootstrap-server localhost:9094 \
  --topic leaderboard.players \
  --property schema.registry.url=https://localhost:8081 \
  --property 'schema.registry.ssl.truststore.location=truststore.jks' \
  --property 'schema.registry.ssl.truststore.password=mystorepassword' \
  --property 'schema.registry.basic.auth.credentials.source=USER_INFO' \
  --property 'schema.registry.basic.auth.user.info=nermin:nermin-secret' \
  --property 'parse.key=true' \
  --property 'value.schema={"type":"record","namespace":"io.nermdev.schemas.avro.leaderboards","name":"Player","fields":[{"name":"id","type":"long"},{"name":"name","type":"string"}]}' \
  --property 'key.serializer=org.apache.kafka.common.serialization.LongSerializer' \
  --producer.config sasl_ssl.properties \
  --property 'key.separator=|' < ./generated_json/players_generated.json
