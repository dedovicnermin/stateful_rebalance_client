package io.nermdev.kafka.stateful_rebalance_client.listener.state;

import io.nermdev.kafka.stateful_rebalance_client.listener.EventListener;
import io.nermdev.kafka.stateful_rebalance_client.model.PayloadOrError;
import lombok.SneakyThrows;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;
import java.util.stream.Collectors;

public abstract class StateEventListener<K, V> implements EventListener<K, V> {
    private final Logger log = LoggerFactory.getLogger(StateEventListener.class);

    protected final Map<TopicPartition, Map<K, V>> state;
    protected final Map<K, TopicPartition> queryState;

    protected StateEventListener() {
        this.state = new HashMap<>();
        this.queryState = new HashMap<>();
    }

    protected void printState(final String id) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        getAppState().forEach((key, value) -> sb.append(key).append(":").append(value).append("\t"));
        sb.append("]");
        log.info("{} : {}", id, sb);
    }

    public void clearState(TopicPartition topicPartition) {
        state.remove(topicPartition);
        final List<Map.Entry<K, TopicPartition>> collect = queryState.entrySet().stream()
                .filter(entry -> entry.getValue().equals(topicPartition)).collect(Collectors.toList());
        collect
                .forEach(entry -> queryState.remove(entry.getKey()));
    }

    protected void put(final TopicPartition tp, final K key, final V value) {
        Optional.ofNullable(state.get(tp))
                .ifPresentOrElse(
                        s -> {
                            s.put(key, value);
                            queryState.put(key, tp);
                        },
                        () -> {
                            state.put(tp, new HashMap<>());
                            state.get(tp).put(key, value);
                            queryState.put(key, tp);
                        }
                );
    }

    public Map<TopicPartition, Map<K, V>> getAppState() {
        return Collections.unmodifiableMap(state);
    }

    @SneakyThrows
    public Optional<V> getValue(final K key) {
        final TopicPartition topicPartition = queryState.get(key);
        return Optional.ofNullable(topicPartition)
                .map(state::get)
                .map(m -> m.get(key));


    }

}
