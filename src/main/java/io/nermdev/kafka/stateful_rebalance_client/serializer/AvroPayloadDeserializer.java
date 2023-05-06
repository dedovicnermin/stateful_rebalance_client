package io.nermdev.kafka.stateful_rebalance_client.serializer;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.nermdev.kafka.stateful_rebalance_client.Application;
import io.nermdev.kafka.stateful_rebalance_client.model.PayloadOrError;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

public class AvroPayloadDeserializer<T> implements Deserializer<PayloadOrError<T>> {
    private static final Logger log = LoggerFactory.getLogger(AvroPayloadDeserializer.class);
    private final KafkaAvroDeserializer deserializer;

    public AvroPayloadDeserializer(KafkaAvroDeserializer deserializer) {
        this.deserializer = deserializer;
    }

    public AvroPayloadDeserializer(final Properties properties) {
        deserializer = new KafkaAvroDeserializer();
        properties.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        deserializer.configure(Application.propertiesToMap(properties), false);
    }

    public AvroPayloadDeserializer(final Map<String, Object> properties) {
        deserializer = new KafkaAvroDeserializer();
        properties.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        deserializer.configure(Collections.unmodifiableMap(properties), false);
    }

    @Override
    public PayloadOrError<T> deserialize(String s, byte[] bytes) {

        final String encoded = new String(bytes, StandardCharsets.UTF_8);
        try {
            final T deserialized = (T) deserializer.deserialize(s, bytes);
            return new PayloadOrError<>(deserialized, null, encoded);
        } catch (Exception e) {
            log.error("Error deserializing product ({}) : {}", s, e.getMessage());
            e.printStackTrace();
            return new PayloadOrError<>(null, e, encoded);
        }
    }
}
