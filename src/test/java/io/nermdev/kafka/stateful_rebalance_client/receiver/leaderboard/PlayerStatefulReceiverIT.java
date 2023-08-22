package io.nermdev.kafka.stateful_rebalance_client.receiver.leaderboard;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.nermdev.kafka.stateful_rebalance_client.framework.AbstractContainerBaseTest;
import io.nermdev.kafka.stateful_rebalance_client.framework.TestSender;
import io.nermdev.kafka.stateful_rebalance_client.listener.state.StateEventListener;
import io.nermdev.kafka.stateful_rebalance_client.model.PayloadOrError;
import io.nermdev.kafka.stateful_rebalance_client.sender.EventSender;
import io.nermdev.kafka.stateful_rebalance_client.serializer.AvroPayloadDeserializer;
import io.nermdev.kafka.stateful_rebalance_client.util.AppClientType;
import io.nermdev.kafka.stateful_rebalance_client.util.ConfigExtractor;
import io.nermdev.kafka.stateful_rebalance_client.util.LeaderboardUtils;
import io.nermdev.schemas.avro.leaderboards.Player;
import org.apache.kafka.clients.consumer.ConsumerConfig;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


class PlayerStatefulReceiverIT extends AbstractContainerBaseTest {
    static final Logger log = LoggerFactory.getLogger(PlayerStatefulReceiverIT.class);
    static final String SR_URL = "schema.registry.url";
    static final String PLAYER_RECEIVER_PREFIX = AppClientType.CONSUMER_PLAYER.getPrefix() + ".";
    static final String LEADERBOARD_PLAYERS_TOPIC = "leaderboard.players";

    static KafkaConsumer<Long, PayloadOrError<Player>> testConsumer;
    static PlayerStatefulReceiver playerReceiver;
    static KafkaProducer<Long, Player> testProducer;
    static EventSender<Long, Player> testSender;

    @BeforeAll
    static void setup() {
        final Map<String, Object> map = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaContainer().getBootstrapServers(),
                SR_URL, getSrContainer().getBaseUrl(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class,
                PLAYER_RECEIVER_PREFIX + ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaContainer().getBootstrapServers(),
                PLAYER_RECEIVER_PREFIX + SR_URL, getSrContainer().getBaseUrl(),
                PLAYER_RECEIVER_PREFIX + ConsumerConfig.GROUP_ID_CONFIG, "test",
                PLAYER_RECEIVER_PREFIX + ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"
        );
        Map<String, Object> clientConfig = new HashMap<>(map);
        testProducer = new KafkaProducer<>(clientConfig);
        testSender = new TestSender<>(testProducer, LEADERBOARD_PLAYERS_TOPIC);
        final AvroPayloadDeserializer<Player> payloadDeserializer = new AvroPayloadDeserializer<>(clientConfig);
        testConsumer = new KafkaConsumer<>(ConfigExtractor.extractConfig(clientConfig, AppClientType.CONSUMER_PLAYER), new LongDeserializer(), payloadDeserializer);
        playerReceiver = new PlayerStatefulReceiver(clientConfig, testConsumer);
    }


    @BeforeEach
    void containerEndpoints() {
        log.debug("Kafka_Endpoint ({})", getKafkaContainer().getBootstrapServers());
        log.debug("SR_Endpoint ({})", getSrContainer().getBaseUrl());
    }

    @AfterAll
    static void cleanup() {
        testSender.close();
        playerReceiver.close();
    }


    @Test
    void test() {
        final Player player1 = Player.newBuilder().setId(1L).setName("Player_1").build();
        testSender.send(player1.getId(), player1);
        final Player player2 = Player.newBuilder().setId(2L).setName("Player_2").build();
        testSender.send(player2.getId(), player2);
        new Thread(playerReceiver).start();

        final StateEventListener<Long, Player> stateListener = playerReceiver.getStateListener();
        Awaitility.waitAtMost(Duration.ofSeconds(2)).untilAsserted(
                () -> Assertions.assertThat(stateListener.getAppState().size()).isNotZero()
        );
        final Map<TopicPartition, Map<Long, Player>> actualState = stateListener.getAppState();
        stateListener.printState(LEADERBOARD_PLAYERS_TOPIC);
        final TopicPartition tp = LeaderboardUtils.tpOf(LEADERBOARD_PLAYERS_TOPIC, 0);
        Assertions.assertThat(actualState).containsKey(tp);
        Assertions.assertThat(actualState.get(tp))
                .isNotNull()
                .hasSize(2)
                .containsKeys(player1.getId(), player2.getId())
                .containsValues(player1, player2);

    }

}