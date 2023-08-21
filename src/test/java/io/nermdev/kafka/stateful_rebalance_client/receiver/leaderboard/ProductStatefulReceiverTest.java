package io.nermdev.kafka.stateful_rebalance_client.receiver.leaderboard;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.nermdev.kafka.stateful_rebalance_client.framework.AbstractContainerBaseTest;
import io.nermdev.kafka.stateful_rebalance_client.framework.TestSender;
import io.nermdev.kafka.stateful_rebalance_client.listener.state.StateEventListener;
import io.nermdev.kafka.stateful_rebalance_client.model.PayloadOrError;
import io.nermdev.kafka.stateful_rebalance_client.sender.EventSender;
import io.nermdev.kafka.stateful_rebalance_client.serializer.AvroPayloadDeserializer;
import io.nermdev.kafka.stateful_rebalance_client.util.LeaderboardUtils;
import io.nermdev.schemas.avro.leaderboards.Product;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


class ProductStatefulReceiverTest extends AbstractContainerBaseTest {
    static final String LEADERBOARD_PRODUCTS_TOPIC = "leaderboard.products";
    static KafkaConsumer<Long, PayloadOrError<Product>> testConsumer;
    static ProductStatefulReceiver productReceiver;
    static KafkaProducer<Long, Product> producer;
    static EventSender<Long, Product> testSender;


    @BeforeAll
    static void setup() {
        final Map<String, Object> bootstrapServersConfig = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaContainer().getBootstrapServers(),
                "schema.registry.url", getSrContainer().getBaseUrl(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class,
                "consumers.product.group.id", "test",
                "consumers.product.enable.auto.commit", "false",
                "consumers.product.schema.registry.url", getSrContainer().getBaseUrl()
        );
        Map<String, Object> map = new HashMap<>(bootstrapServersConfig);
        final AvroPayloadDeserializer<Product> payloadDeserializer = new AvroPayloadDeserializer<>(map);
        producer = new KafkaProducer<>(map);
        testSender = new TestSender<>(producer, LEADERBOARD_PRODUCTS_TOPIC);


        testConsumer = new KafkaConsumer<>(map, new LongDeserializer(), payloadDeserializer);
        productReceiver = new ProductStatefulReceiver(map, testConsumer);
    }

    @AfterAll
    static void cleanup(){
        testSender.close();
        productReceiver.close();
        testConsumer.close();
    }

    @BeforeEach
    void printContainerEndpoints() {
        System.out.println(getKafkaContainer().getBootstrapServers());
        System.out.printf(getSrContainer().getBaseUrl());
    }

    @Test
    void test() {
        final Product product1 = Product.newBuilder().setId(1L).setName("Product_1").build();
        testSender.send(product1.getId(), product1);
        final Product product2 = Product.newBuilder().setId(2L).setName("Product_2").build();
        testSender.send(product2.getId(), product2);
        new Thread(productReceiver).start();

        final StateEventListener<Long, Product> stateListener = productReceiver.getStateListener();
        Awaitility.waitAtMost(Duration.ofSeconds(2)).untilAsserted(
                () -> Assertions.assertThat(stateListener.getAppState().size()).isNotZero()
        );
        final Map<TopicPartition, Map<Long, Product>> appState = stateListener.getAppState();
        stateListener.printState("PRODUCT_STATE");
        final TopicPartition tp = LeaderboardUtils.tpOf(LEADERBOARD_PRODUCTS_TOPIC, 0);
        Assertions.assertThat(appState).containsKey(tp);
        Assertions.assertThat(appState.get(tp))
                .hasSize(2)
                .containsKeys(product1.getId(), product2.getId())
                .containsValues(product1, product2);


    }
}