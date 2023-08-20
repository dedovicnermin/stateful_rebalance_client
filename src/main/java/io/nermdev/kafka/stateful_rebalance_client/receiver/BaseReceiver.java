package io.nermdev.kafka.stateful_rebalance_client.receiver;

import io.nermdev.kafka.stateful_rebalance_client.model.PayloadOrError;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;


import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;


public abstract class BaseReceiver<K, V> extends AbstractReceiver<K, V>  {
    protected final Map<String, Object> consumerConfig;
    protected final CountDownLatch countDownLatch;
    protected final Duration pollDuration;
    protected final String topic;

    protected BaseReceiver(final Map<String, Object> consumerConfig) {
        this.countDownLatch = new CountDownLatch(1);
        this.consumerConfig = consumerConfig;
        this.pollDuration = getPollDuration();
        topic = setTopic();

    }

    private String setTopic() {
        return (String) consumerConfig.getOrDefault("topic", getTopicName());
    }

    private Duration getPollDuration() {
        return Optional.ofNullable((String)consumerConfig.get("poll.duration"))
                .map(Long::parseLong)
                .map(Duration::ofMillis)
                .orElse(Duration.ofMillis(500));
    }

    protected abstract String getTopicName();

    protected abstract KafkaConsumer<K, PayloadOrError<V>> getConsumer();

    protected abstract Logger getLogger();


    @Override
    public void start() {
        run();
    }

    @Override
    public void close() {
        getLogger().info("BaseReceiver.close() invoked. Waking up consumer");
        getConsumer().wakeup();
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            getLogger().error("Interrupted exception catch block (EXCEPTION) : {}", e.getMessage());
            throw new RuntimeException("Interrupt occurred during receiver shutdown \n",e);
        }
        getLogger().info("Receiver and consumer closed");
    }




}
