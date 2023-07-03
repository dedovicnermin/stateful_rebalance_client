package io.nermdev.kafka.stateful_rebalance_client.receiver;

import io.nermdev.kafka.stateful_rebalance_client.model.PayloadOrError;

import io.nermdev.kafka.stateful_rebalance_client.util.AppClientType;
import io.nermdev.kafka.stateful_rebalance_client.util.ConfigExtractor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CountDownLatch;


public abstract class BaseReceiver<K, V> extends AbstractReceiver<K, V> {
    private final Logger log = LoggerFactory.getLogger(BaseReceiver.class);
    protected final String topic;
    protected final CountDownLatch countDownLatch;
    protected final Map<String, Object> consumerConfig;

    protected BaseReceiver(final Map<String, Object> consumerConfig) {
        countDownLatch = new CountDownLatch(1);
        topic = getTopicName(consumerConfig);
        this.consumerConfig = ConfigExtractor.extractConfig(consumerConfig, AppClientType.CONSUMER, getConfigKey());
    }
    protected abstract String getTopicName(final Map<String, Object> config);

    protected abstract String getConfigKey();
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
            throw new RuntimeException("Interrupt occurred during receiver shutdown \n",e);
        }
        log.info("Receiver and consumer closed");
    }




}
