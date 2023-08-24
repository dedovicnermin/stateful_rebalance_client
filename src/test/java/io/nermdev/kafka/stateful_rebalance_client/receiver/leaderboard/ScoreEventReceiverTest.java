package io.nermdev.kafka.stateful_rebalance_client.receiver.leaderboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.nermdev.kafka.stateful_rebalance_client.framework.AbstractContainerBaseTest;
import io.nermdev.kafka.stateful_rebalance_client.framework.TestListener;
import io.nermdev.kafka.stateful_rebalance_client.framework.TestReceiver;
import io.nermdev.kafka.stateful_rebalance_client.framework.TestSender;
import io.nermdev.kafka.stateful_rebalance_client.listener.ScoreEventListener;
import io.nermdev.kafka.stateful_rebalance_client.listener.state.PlayerStateListener;
import io.nermdev.kafka.stateful_rebalance_client.listener.state.ProductStateListener;
import io.nermdev.kafka.stateful_rebalance_client.model.PayloadOrError;
import io.nermdev.kafka.stateful_rebalance_client.receiver.ReceiveEvent;
import io.nermdev.kafka.stateful_rebalance_client.sender.EventSender;
import io.nermdev.kafka.stateful_rebalance_client.sender.ScoreCardSender;
import io.nermdev.kafka.stateful_rebalance_client.serializer.AvroPayloadDeserializer;
import io.nermdev.kafka.stateful_rebalance_client.util.AppClientType;
import io.nermdev.kafka.stateful_rebalance_client.util.ConfigExtractor;
import io.nermdev.kafka.stateful_rebalance_client.util.LeaderboardUtils;
import io.nermdev.schemas.avro.leaderboards.Player;
import io.nermdev.schemas.avro.leaderboards.Product;
import io.nermdev.schemas.avro.leaderboards.ScoreCard;
import io.nermdev.schemas.avro.leaderboards.ScoreEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.mockito.Mockito.when;

class ScoreEventReceiverTest extends AbstractContainerBaseTest {

    static final Logger log = LoggerFactory.getLogger(ScoreEventReceiverTest.class);
    static final String SR_URL = "schema.registry.url";
    static final String SCORE_RECEIVER_PREFIX = AppClientType.CONSUMER_SCORE.getPrefix() + ".";
    static final String SCORECARD_SENDER_PREFIX = AppClientType.PRODUCER_SCORECARD.getPrefix() + ".";
    static final String LEADERBOARD_SCOREEVENT_TOPIC = ScoreEventReceiver.LEADERBOARD_SCORES_TOPIC;
    static final String LEADERBOARD_SCORECARD_TOPIC = ScoreCardSender.LEADERBOARD_SCORECARDS_TOPIC;
    static KafkaConsumer<Long, PayloadOrError<ScoreEvent>> consumerScoreEvent;
    static KafkaConsumer<Long, PayloadOrError<ScoreCard>> testConsumer;
    static TestReceiver<Long, ScoreCard> testReceiver;
    static TestListener<Long, ScoreCard> testListener;


    static ScoreEventReceiver receiverScoreEvent;
    static ScoreEventListener listenerScoreEvent;
    static KafkaProducer<Long, ScoreEvent> testProducerScoreEvent;
    static TestSender<Long, ScoreEvent> testSenderScoreEvent;
    static ScoreCardSender scoreCardSender;



    private static final PlayerStatefulReceiver mockPlayerReceiver = Mockito.mock(PlayerStatefulReceiver.class);
    private static final PlayerStateListener mockPlayerListener = Mockito.mock(PlayerStateListener.class);
    private static final ProductStateListener mockProductListener = Mockito.mock(ProductStateListener.class);

    static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @BeforeAll
    static void setup() {
        final Map<String, Object> map = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaContainer().getBootstrapServers(),
                SR_URL, getSrContainer().getBaseUrl(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        final Map<String, Object> clientMap = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaContainer().getBootstrapServers(),
                SR_URL, getSrContainer().getBaseUrl(),
                SCORE_RECEIVER_PREFIX + ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaContainer().getBootstrapServers(),
                SCORE_RECEIVER_PREFIX + SR_URL, getSrContainer().getBaseUrl(),
                SCORE_RECEIVER_PREFIX + ConsumerConfig.GROUP_ID_CONFIG, "test",
                SCORE_RECEIVER_PREFIX + ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false",
                SCORECARD_SENDER_PREFIX + ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaContainer().getBootstrapServers(),
                SCORECARD_SENDER_PREFIX + SR_URL, getSrContainer().getBaseUrl(),
                SCORECARD_SENDER_PREFIX +ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class,
                SCORECARD_SENDER_PREFIX + ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class
        );
        Map<String, Object> clientConfig = new HashMap<>(clientMap);
        clientConfig.put(SCORE_RECEIVER_PREFIX + ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        log.info("NERMIN : Creating external producer for sending ScoreEvent");
        testProducerScoreEvent = new KafkaProducer<>(map);
        log.info("NERMIN : Creating TestSender for sending ScoreEvent");
        testSenderScoreEvent = new TestSender<>(testProducerScoreEvent, LEADERBOARD_SCOREEVENT_TOPIC);
        final AvroPayloadDeserializer<ScoreEvent> payloadDeserializer = new AvroPayloadDeserializer<>(new HashMap<>(clientConfig));


        final Map<String, Object> consumerConfig = ConfigExtractor.extractConfig(clientConfig, AppClientType.CONSUMER_SCORE);
        log.info("NERMIN : Extracted Config for CONSUMER_SCORE : \n{}", gson.toJson(consumerConfig));

        log.info("NERMIN : Creating KafkaConsumer responsible for subscribing to {}", LEADERBOARD_SCOREEVENT_TOPIC);
        consumerScoreEvent = new KafkaConsumer<>(consumerConfig, new LongDeserializer(), payloadDeserializer);
        log.info("NERMIN : Creating ScoreEventReceiver with mockPlayerReceiver");
        receiverScoreEvent = new ScoreEventReceiver(consumerConfig, mockPlayerReceiver, consumerScoreEvent);

        final Map<String, Object> scoreCardSenderConfig = ConfigExtractor.extractConfig(clientConfig, AppClientType.PRODUCER_SCORECARD);
        log.info("NERMIN : Extracted Config for ScoreCardSend : \n{}", scoreCardSenderConfig);

        log.info("NERMIN : Creating ScoreCardSender...");
        scoreCardSender = new ScoreCardSender(new HashMap<>(scoreCardSenderConfig));

        log.info("NERMIN : Creating ScoreEventListener with mockProductListener and mockPlayerListener");
        listenerScoreEvent = new ScoreEventListener(receiverScoreEvent, mockProductListener, mockPlayerListener, scoreCardSender);

        final Map<String, Object> testConsumerConfig = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaContainer().getBootstrapServers(),
                SR_URL, getSrContainer().getBaseUrl(),
                ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
        );
        log.info("NERMIN : Consumer Config for testConsumer responsible for consuming from {} : \n{}", LEADERBOARD_SCORECARD_TOPIC, testConsumerConfig );
        final AvroPayloadDeserializer<ScoreCard> scoreCardAvroPayloadDeserializer = new AvroPayloadDeserializer<>(new HashMap<>(testConsumerConfig));
        testConsumer = new KafkaConsumer<>(
                testConsumerConfig,
                new LongDeserializer(),
                scoreCardAvroPayloadDeserializer
        );
        log.info("NERMIN : Creating Test Receiver for firing ScoreCards");
        testReceiver = new TestReceiver<>(testConsumer, Duration.ofSeconds(1L), LEADERBOARD_SCORECARD_TOPIC);
        log.info("NERMIN : Creating TestListener");
        testListener = new TestListener<>(testReceiver);

    }


    static final Player player1 = Player.newBuilder().setId(1L).setName("Player_1").build();
    static final Player player2 = Player.newBuilder().setId(2L).setName("Player_2").build();
    static final Product product = Product.newBuilder().setId(100L).setName("COD").build();

    @BeforeEach
    void containerEndpoints() {
        log.debug("Kafka_Endpoint ({})", getKafkaContainer().getBootstrapServers());
        log.debug("SR_Endpoint ({})", getSrContainer().getBaseUrl());
        defineMockBehavior();
    }

    private static void defineMockBehavior() {
        when(mockPlayerReceiver.getStateListener()).thenReturn(mockPlayerListener);
        final TopicPartition topicPartition = LeaderboardUtils.tpOf(PlayerStatefulReceiver.PLAYERS_TOPIC, 0);
        when(mockPlayerReceiver.getCurrAssignment()).thenReturn(Collections.singleton(topicPartition));
        when(mockPlayerListener.getAppState()).thenReturn(
                Map.of(
                        topicPartition,
                        Map.of(
                                player1.getId(), player1,
                                player2.getId(), player2
                        )
                )
        );
        when(mockPlayerListener.getValue(player1.getId())).thenReturn(Optional.of(player1));
        when(mockPlayerListener.getValue(player2.getId())).thenReturn(Optional.of(player2));
        when(mockProductListener.getValue(product.getId())).thenReturn(Optional.of(product));
    }

    @AfterAll
    static void cleanup() {
        testSenderScoreEvent.close();
        receiverScoreEvent.close();
        scoreCardSender.close();
        testReceiver.close();
    }




    @Test
    void test() throws EventSender.SendException {
        final ScoreEvent player1Score = ScoreEvent.newBuilder().setPlayerId(player1.getId()).setProductId(product.getId()).setScore(50).setDate("now").build();
        testSenderScoreEvent.blockingSend(player1.getId(),player1Score);
        final ScoreEvent player2Score = ScoreEvent.newBuilder().setPlayerId(player2.getId()).setProductId(product.getId()).setScore(100).setDate("now").build();
        testSenderScoreEvent.blockingSend(player2.getId(),player2Score);
        new Thread(receiverScoreEvent).start();


        testReceiver.start();
        Assertions.assertThat(testListener.getActualSize()).isNotZero();
        Awaitility.waitAtMost(Duration.ofSeconds(30)).untilAsserted(
                () -> {
                    testReceiver.start();
                    Assertions.assertThat(testListener.getActualSize()).isEqualTo(2);
                }
        );
        final List<ReceiveEvent<Long, ScoreCard>> actual = testListener.getActual();
        actual.forEach(
                event -> log.info(event.toString())
        );
        Assertions.assertThat(actual).hasSize(2);
        final List<ScoreCard> actualScoreCards = actual.stream().map(
                ReceiveEvent::getPayload
        ).collect(Collectors.toList());
        final ScoreCard expectedScoreCard1 = ScoreCard.newBuilder().setPlayer(new Player(player1.getId(), player1.getName())).setProduct(new Product(product.getId(), product.getName())).setLatestDate("now").setScore(player1Score.getScore()).build();
        final ScoreCard expectedScoreCard2 = ScoreCard.newBuilder().setPlayer(new Player(player2.getId(), player2.getName())).setProduct(new Product(product.getId(), product.getName())).setLatestDate("now").setScore(player2Score.getScore()).build();

        Assertions.assertThat(actualScoreCards).contains(expectedScoreCard1, expectedScoreCard2);


    }



}