package io.nermdev.kafka.stateful_rebalance_client.serializer;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.nermdev.schemas.avro.leaderboards.ScoreCard;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ScoreCardSerializer implements Serializer<ScoreCard> {
    private static final Logger log = LoggerFactory.getLogger(ScoreCardSerializer.class);
    private final KafkaAvroSerializer serializer;
    public ScoreCardSerializer(Map<String, Object> configs) {
        this.serializer = new KafkaAvroSerializer();
        this.serializer.configure(configs, false);
    }

    public ScoreCardSerializer() {
        this.serializer = new KafkaAvroSerializer();
    }
    @Override
    public byte[] serialize(String s, ScoreCard scoreCard) {
        try {
            log.debug("Serializing : {}:{}", s, scoreCard);
            return serializer.serialize(s, scoreCard);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Something unexpected happended during serializion : {}", e.getMessage());
            throw new RuntimeException(e);

        }
    }
}
