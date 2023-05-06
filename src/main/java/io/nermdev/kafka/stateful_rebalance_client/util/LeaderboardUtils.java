package io.nermdev.kafka.stateful_rebalance_client.util;


import io.nermdev.kafka.stateful_rebalance_client.model.PayloadOrError;
import io.nermdev.kafka.stateful_rebalance_client.receiver.ReceiveEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

public class LeaderboardUtils {
    private static Logger log = LoggerFactory.getLogger(LeaderboardUtils.class);
    public static <K, V> ReceiveEvent<K, V> createReceiveEvent(final ConsumerRecord<K, PayloadOrError<V>> record) {
        return Optional.ofNullable(record.value())
                .map(PayloadOrError::getPayload)
                .map(v -> new ReceiveEvent<>(record.key(), v, null, record, record.value().getEncodedValue()))
                .orElseGet(() -> new ReceiveEvent<>(record.key(), null, record.value().getError(), record, record.value().getEncodedValue()));
    }

    public static TopicPartition tpOf(final String topic, final int partition) {
        return new TopicPartition(topic, partition);
    }

    /**
     * Liveness and safety. Kafka satifies via :
     * 1. checking availability : consumer heartbeats. If consumer doesnt hearbeat within time specified in `session.timeout.ms`, will presume consumer to be dead.
     * 2. checking progress : consumer req. to invoke 'poll()', thereby indicating that its able to handle its share of the partition assignment
     * DEFAULTS:
     * `session.timeout.ms`: 10s - should be 3x > `heartbeat.interval.ms`. Must be in range `group.min.session.timout.ms` and `group.max.session.timeout.ms` broker props
     * `hearbeat.interval.ms`: 3s
     * `group.min.session.timeout.ms` : 6s
     * `group.max.session.timeout.ms` : 30min
     * `max.poll.interval.ms` : 5min
     * `max.poll.records` : 500
     *
     * NOTE: client should assign session.timeout.ms and heartbeat.interval.ms values based on the applications appetite for failure detection.
     * Shorter == quicker dead consumer detection
     *
     *
     * @param consumerConfig
     * @param component
     */
    public static void configureForK8(final Map<String, Object> consumerConfig, final String component) {
        final String podName = System.getenv().get("POD_NAME");
        Optional.ofNullable(podName)
                .ifPresent(name -> {
                    log.info("Kubernetes environment detected for component {}. Updating config...", component);
//                    consumerConfig.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, name + "-" + component);
                    consumerConfig.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15000);
                    consumerConfig.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 4000);
                });
    }
}
