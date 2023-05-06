package io.nermdev.kafka.stateful_rebalance_client.receiver;




import io.nermdev.kafka.stateful_rebalance_client.listener.EventListener;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractReceiver<K, V> implements EventReceiver<K, V>, Runnable{

    private final Set<EventListener<K,V>> listeners = new HashSet<>();

    @Override
    public void addListener(EventListener<K,V> listener) {
        listeners.add(listener);
    }

    protected final void fire(final ReceiveEvent<K, V> event) {
        for (var listener : listeners) {
            listener.onEvent(event);
        }
    }


}
