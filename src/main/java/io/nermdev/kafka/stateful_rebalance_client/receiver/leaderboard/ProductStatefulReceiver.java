package io.nermdev.kafka.stateful_rebalance_client.receiver.leaderboard;

import io.nermdev.kafka.stateful_rebalance_client.listener.state.ProductStateListener;
import io.nermdev.kafka.stateful_rebalance_client.listener.state.StateEventListener;
import io.nermdev.kafka.stateful_rebalance_client.model.PayloadOrError;
import io.nermdev.kafka.stateful_rebalance_client.receiver.BaseStatefulReceiver;
import io.nermdev.kafka.stateful_rebalance_client.receiver.ReceiveEvent;
import io.nermdev.kafka.stateful_rebalance_client.util.LeaderboardUtils;
import io.nermdev.schemas.avro.leaderboards.Product;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

public class ProductStatefulReceiver extends BaseStatefulReceiver<Long, Product> {

    private static final Logger log = LoggerFactory.getLogger(ProductStatefulReceiver.class);
    private final ProductStateListener stateListener;
    private final KafkaConsumer<Long, PayloadOrError<Product>> consumer;


    public ProductStatefulReceiver(final Map<String, Object> consumerConfig, final KafkaConsumer<Long, PayloadOrError<Product>> consumer) {
        super(consumerConfig);
        this.consumer = consumer;
        this.stateListener = new ProductStateListener(this);
    }


    @Override
    public void run() {
        consumerConfig.put("client.id", consumerConfig.get("client.id") + "-" + System.getenv("POD_NAME"));
        final TopicPartition tp = new TopicPartition(topic, 0);
        consumer.assign(Collections.singleton(tp));
        consumer.seekToBeginning(Collections.singleton(tp));



        try {
            while (true) {
                final ConsumerRecords<Long, PayloadOrError<Product>> consumerRecords = consumer.poll(pollDuration);
                for (ConsumerRecord<Long, PayloadOrError<Product>> cr : consumerRecords) {
                    final ReceiveEvent<Long, Product> productReceiveEvent = LeaderboardUtils.createReceiveEvent(cr);
                    fire(productReceiveEvent);
                }
            }
        } catch (WakeupException e) {
            log.info("Consumer poll woke up");
        } catch (Exception e) {
          log.error("Something unexpected happened : {}", e.getMessage());
        } finally {
            try {
                log.info("Shutting down product receiver");
            } finally {
                log.info("Gracefully closing product receiver consumer...");
                consumer.close();
                countDownLatch.countDown();
                log.info("The ProductReceiver consumer is now gracefully closed");
            }
        }
    }


    @Override
    protected String getTopicName() {
        return "leaderboard.products";
    }


    @Override
    protected KafkaConsumer<Long, PayloadOrError<Product>> getConsumer() {
        return consumer;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    public StateEventListener<Long, Product> getStateListener() {
        return stateListener;
    }
}
