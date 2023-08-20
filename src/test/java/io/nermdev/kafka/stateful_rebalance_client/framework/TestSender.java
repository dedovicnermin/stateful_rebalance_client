package io.nermdev.kafka.stateful_rebalance_client.framework;

import io.nermdev.kafka.stateful_rebalance_client.sender.EventSender;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.concurrent.Future;

public class TestSender<K, V> implements EventSender<K, V> {

    final KafkaProducer<K, V> producer;
    final String topic;

    public TestSender(KafkaProducer<K, V> producer, String topic) {
        this.producer = producer;
        this.topic = topic;
    }

    @Override
    public Future<RecordMetadata> send(V payload) {
        return null;
    }

    @Override
    public Future<RecordMetadata> send(K key, V payload) {
        return null;
    }

    @Override
    public void close() {

    }
}
