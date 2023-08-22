package io.nermdev.kafka.stateful_rebalance_client.receiver;



import io.nermdev.kafka.stateful_rebalance_client.listener.state.StateEventListener;

import java.util.Map;

public abstract class BaseStatefulReceiver<K,V> extends BaseReceiver<K, V> {

    protected BaseStatefulReceiver(Map<String, Object> consumerConfig) {
        super(consumerConfig);
    }

    public abstract StateEventListener<K, V> getStateListener();
}
