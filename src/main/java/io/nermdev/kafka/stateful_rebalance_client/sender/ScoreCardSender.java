package io.nermdev.kafka.stateful_rebalance_client.sender;


import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import io.nermdev.kafka.stateful_rebalance_client.serializer.ScoreCardSerializer;
import io.nermdev.schemas.avro.leaderboards.ScoreCard;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.LongSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

public class ScoreCardSender implements EventSender<Long, ScoreCard> {
    private static final Logger log = LoggerFactory.getLogger(ScoreCardSender.class);
    public static final String LEADERBOARD_SCORECARDS_TOPIC = "leaderboard.scorecards";
    private final KafkaProducer<Long, ScoreCard> producer;
    private final String topic;

    public ScoreCardSender(final Map<String, Object> producerConfig) {
        this.topic = LEADERBOARD_SCORECARDS_TOPIC;
        producerConfig.put("key.serializer", LongSerializer.class);

        producerConfig.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        producerConfig.put(ProducerConfig.ACKS_CONFIG, "all");
        producerConfig.put(KafkaAvroSerializerConfig.AVRO_REFLECTION_ALLOW_NULL_CONFIG, true);
        producer = new KafkaProducer<>(producerConfig, new LongSerializer(), new ScoreCardSerializer(producerConfig));
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
        log.debug("Sending Event : {} -- {}", key, payload);
        return producer.send(new ProducerRecord<>(topic, key, payload));
    }

    @Override
    public void close() {
        producer.close();
    }
}
