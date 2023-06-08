package io.nermdev.kafka.stateful_rebalance_client.receiver;

import io.confluent.connect.replicator.offsets.ConsumerTimestampsInterceptor;
import io.confluent.monitoring.clients.interceptor.MonitoringConsumerInterceptor;
import io.nermdev.kafka.stateful_rebalance_client.model.PayloadOrError;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;


public abstract class BaseReceiver<K, V> extends AbstractReceiver<K, V> {
    private final Logger log = LoggerFactory.getLogger(BaseReceiver.class);
    protected final String topic;
    protected final CountDownLatch countDownLatch;
    protected final Map<String, Object> consumerConfig;

    protected BaseReceiver(final Map<String, Object> consumerConfig) {
        countDownLatch = new CountDownLatch(1);
        consumerConfig.put("group.id", getGroupName(consumerConfig));
        consumerConfig.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, Arrays.asList(ConsumerTimestampsInterceptor.class, MonitoringConsumerInterceptor.class));
        topic = getTopicName(consumerConfig);
        this.consumerConfig = consumerConfig;
    }

    protected abstract String getGroupName(final Map<String, Object> config);
    protected abstract String getTopicName(final Map<String, Object> config);
    protected abstract KafkaConsumer<K, PayloadOrError<V>> getConsumer();


    @Override
    public void start() {
        run();
    }

    @Override
    public void close() {
        log.info("BaseReceiver.close() invoked. Waking up consumer");
        getConsumer().wakeup();
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("Interrupted exception catch block (EXCEPTION) : {}", e.getMessage());
            throw new RuntimeException(e);
        }
        log.info("Receiver and consumer closed");
    }




}
