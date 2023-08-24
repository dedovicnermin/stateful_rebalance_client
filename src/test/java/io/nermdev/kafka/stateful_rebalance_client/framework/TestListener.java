package io.nermdev.kafka.stateful_rebalance_client.framework;



import io.nermdev.kafka.stateful_rebalance_client.listener.EventListener;
import io.nermdev.kafka.stateful_rebalance_client.model.PayloadOrError;
import io.nermdev.kafka.stateful_rebalance_client.receiver.EventReceiver;
import io.nermdev.kafka.stateful_rebalance_client.receiver.ReceiveEvent;

import java.util.ArrayList;
import java.util.List;

public class TestListener<K, V> implements EventListener<K,V> {
    private final List<ReceiveEvent<K, V>> actual = new ArrayList<>();

    public TestListener(EventReceiver<K, V> receiver) {
        receiver.addListener(this);
    }

    @Override
    public void onEvent(ReceiveEvent<K,V> event) {
        actual.add(event);
    }

    public List<ReceiveEvent<K,V>> getActual() {
        return actual;
    }

    public int getActualSize() {
        return actual.size();
    }
}
