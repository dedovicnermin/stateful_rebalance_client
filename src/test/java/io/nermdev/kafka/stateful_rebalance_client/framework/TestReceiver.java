package io.nermdev.kafka.stateful_rebalance_client.framework;



import io.nermdev.kafka.stateful_rebalance_client.model.PayloadOrError;
import io.nermdev.kafka.stateful_rebalance_client.receiver.AbstractReceiver;
import io.nermdev.kafka.stateful_rebalance_client.receiver.ReceiveEvent;
import io.nermdev.kafka.stateful_rebalance_client.util.LeaderboardUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.InterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

public class TestReceiver<K, V> extends AbstractReceiver<K, V> {
    static final Logger log = LoggerFactory.getLogger(TestReceiver.class);
    private final Consumer<K, PayloadOrError<V>> consumer;
    private final Duration pollTimeout;

    public TestReceiver(final Consumer<K, PayloadOrError<V>> consumer, final Duration pollTimeout, final String topic)  {
        this.pollTimeout = pollTimeout;
        this.consumer = consumer;
        consumer.subscribe(Collections.singleton(topic));

    }

    @Override
    public void start() {
        run();
    }

    @Override
    public void run() {
        AtomicReference<ConsumerRecords<K, PayloadOrError<V>>> records = new AtomicReference<>();
        try {
            records.set(consumer.poll(pollTimeout));
            if (records.get().isEmpty()) {
                Awaitility.waitAtMost(Duration.ofSeconds(15)).until(() -> {
                    records.set(consumer.poll(pollTimeout));
                    return !records.get().isEmpty();
                });
            }

            if (! records.get().isEmpty()) {
                for (var record : records.get()) {
                    final ReceiveEvent<K, V> event = LeaderboardUtils.createReceiveEvent(record);
                    log.info(event.toString());
                    fire(event);
                }
                consumer.commitSync();
            }
        } catch (InterruptException exception) {
            exception.printStackTrace();
            log.error("NERM - {}", exception.getMessage());
        }
    }

    @Override
    public void close() {
        consumer.close();
    }
}
