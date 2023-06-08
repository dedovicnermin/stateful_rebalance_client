package io.nermdev.kafka.stateful_rebalance_client.sender;


import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import io.confluent.monitoring.clients.interceptor.MonitoringProducerInterceptor;
import io.nermdev.kafka.stateful_rebalance_client.serializer.ScoreCardSerializer;
import io.nermdev.schemas.avro.leaderboards.ScoreCard;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.LongSerializer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

public class ScoreCardSender implements EventSender<Long, ScoreCard> {
    private final Producer<Long, ScoreCard> producer;
    private final String topic;

    public ScoreCardSender(final Map<String, Object> producerConfig) {
        this.topic = "leaderboard.scorecards";
        Map<String, Object> map = new HashMap<>();
        map.put("key.serializer", LongSerializer.class);

        map.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        map.put(ProducerConfig.ACKS_CONFIG, "all");
        map.putAll(producerConfig);
        map.remove("interceptor.classes");
        map.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, Collections.singletonList(MonitoringProducerInterceptor.class));
        map.put(KafkaAvroSerializerConfig.AVRO_REFLECTION_ALLOW_NULL_CONFIG, true);
        producer = new KafkaProducer<>(map, new LongSerializer(), new ScoreCardSerializer(map));
    }

    @Override
    public Future<RecordMetadata> send(ScoreCard payload) {
        final ProducerRecord<Long, ScoreCard> producerRecord = new ProducerRecord<>(topic, payload.getPlayer().getId(), payload);
        producerRecord.headers().add("uuid", UUID.randomUUID().toString().getBytes());
        return producer.send(
                producerRecord
        );
    }

    @Override
    public Future<RecordMetadata> send(Long key, ScoreCard payload) {
        return producer.send(new ProducerRecord<>(topic, key, payload));
    }

    @Override
    public void close() {
        producer.close();
    }
}
