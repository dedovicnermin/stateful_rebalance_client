package io.nermdev.kafka.stateful_rebalance_client.listener;


import io.nermdev.kafka.stateful_rebalance_client.receiver.ReceiveEvent;

@FunctionalInterface
public interface EventListener<K, V> {
    void onEvent(ReceiveEvent<K, V> event);
}
