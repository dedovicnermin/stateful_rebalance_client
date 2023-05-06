package io.nermdev.kafka.stateful_rebalance_client.receiver;


import io.nermdev.kafka.stateful_rebalance_client.model.PayloadOrError;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Data
@AllArgsConstructor
public class ReceiveEvent<K, V> {
    private final K key;
    private final V payload;
    private final Throwable error;
    private final ConsumerRecord<K, PayloadOrError<V>> record;
    private final String encodedValue;
}
