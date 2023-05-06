package io.nermdev.kafka.stateful_rebalance_client.receiver;



import io.nermdev.kafka.stateful_rebalance_client.listener.EventListener;

import java.io.Closeable;

public interface EventReceiver<K, V> extends Closeable {
    void addListener(EventListener<K, V> listener);
    void start();
    void close();
}
