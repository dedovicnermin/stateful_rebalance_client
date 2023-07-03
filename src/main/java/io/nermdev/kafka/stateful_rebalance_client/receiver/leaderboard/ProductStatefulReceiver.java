package io.nermdev.kafka.stateful_rebalance_client.receiver.leaderboard;

import io.nermdev.kafka.stateful_rebalance_client.listener.state.ProductStateListener;
import io.nermdev.kafka.stateful_rebalance_client.listener.state.StateEventListener;
import io.nermdev.kafka.stateful_rebalance_client.model.PayloadOrError;
import io.nermdev.kafka.stateful_rebalance_client.receiver.BaseStatefulReceiver;
import io.nermdev.kafka.stateful_rebalance_client.receiver.ReceiveEvent;
import io.nermdev.kafka.stateful_rebalance_client.serializer.AvroPayloadDeserializer;
import io.nermdev.kafka.stateful_rebalance_client.util.LeaderboardUtils;
import io.nermdev.schemas.avro.leaderboards.Product;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

public class ProductStatefulReceiver extends BaseStatefulReceiver<Long, Product> {

    private static final Logger log = LoggerFactory.getLogger(ProductStatefulReceiver.class);
    private final ProductStateListener stateListener;
    private KafkaConsumer<Long, PayloadOrError<Product>> consumer;



    public ProductStatefulReceiver(final Map<String, Object> consumerConfig) {
        super(consumerConfig);
        this.stateListener = new ProductStateListener(this);
    }


    @Override
    public void run() {
        final LongDeserializer longDeserializer = new LongDeserializer();
        final AvroPayloadDeserializer<Product> avroPayloadDeserializer = new AvroPayloadDeserializer<>(consumerConfig);
        consumerConfig.put("client.id", consumerConfig.get("client.id") + "-" + System.getenv("POD_NAME"));
        consumer = new KafkaConsumer<>(consumerConfig, longDeserializer, avroPayloadDeserializer);
        final TopicPartition tp = new TopicPartition(getTopicName(consumerConfig), 0);
        consumer.assign(Collections.singleton(tp));
        consumer.seekToBeginning(Collections.singleton(tp));

        final Duration pto = Duration.ofMillis(500);

        try {
            while (true) {
                final ConsumerRecords<Long, PayloadOrError<Product>> consumerRecords = consumer.poll(pto);
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
            avroPayloadDeserializer.close();
        }
    }


    @Override
    protected String getTopicName(Map<String, Object> config) {
        return (String) config.getOrDefault("products.topic", "leaderboard.products");
    }

    @Override
    protected String getConfigKey() {
        return "product";
    }

    @Override
    protected KafkaConsumer<Long, PayloadOrError<Product>> getConsumer() {
        return consumer;
    }

    @Override
    public StateEventListener<Long, Product> getStateListener() {
        return stateListener;
    }
}
