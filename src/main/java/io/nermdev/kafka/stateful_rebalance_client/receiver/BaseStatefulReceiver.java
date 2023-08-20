package io.nermdev.kafka.stateful_rebalance_client.receiver;



import io.nermdev.kafka.stateful_rebalance_client.listener.state.StateEventListener;
import io.nermdev.kafka.stateful_rebalance_client.model.PayloadOrError;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Map;

public abstract class BaseStatefulReceiver<K,V> extends BaseReceiver<K, V> {

    protected BaseStatefulReceiver(Map<String, Object> consumerConfig) {
        super(consumerConfig);
    }

    public abstract StateEventListener<K, V> getStateListener();
}
