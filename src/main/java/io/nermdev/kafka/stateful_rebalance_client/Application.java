package io.nermdev.kafka.stateful_rebalance_client;



import io.nermdev.kafka.stateful_rebalance_client.listener.ScoreEventListener;
import io.nermdev.kafka.stateful_rebalance_client.listener.state.PlayerStateListener;
import io.nermdev.kafka.stateful_rebalance_client.listener.state.ProductStateListener;
import io.nermdev.kafka.stateful_rebalance_client.receiver.leaderboard.PlayerStatefulReceiver;
import io.nermdev.kafka.stateful_rebalance_client.receiver.leaderboard.ProductStatefulReceiver;
import io.nermdev.kafka.stateful_rebalance_client.receiver.leaderboard.ScoreEventReceiver;
import io.nermdev.kafka.stateful_rebalance_client.sender.ScoreCardSender;
import io.nermdev.kafka.stateful_rebalance_client.serializer.AvroPayloadDeserializer;
import io.nermdev.kafka.stateful_rebalance_client.util.AppClientType;
import io.nermdev.kafka.stateful_rebalance_client.util.ConfigExtractor;
import io.nermdev.kafka.stateful_rebalance_client.util.ConsumerCloser;
import io.nermdev.schemas.avro.leaderboards.Player;
import io.nermdev.schemas.avro.leaderboards.Product;
import io.nermdev.schemas.avro.leaderboards.ScoreEvent;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    public static final LongDeserializer longDeserializer = new LongDeserializer();

    @SneakyThrows
    public static void main(String[] args) {
        if (args.length < 1) throw new IllegalArgumentException("Pass path to application.properties");
        final Properties properties = new Properties();
        loadConfig(args[0], properties);
        log.info("APP CONFIG : {}", properties);

        final Map<String, Object> appConfig = propertiesToMap(properties);
        final Map<String, Object> productReceiverConfig = extractClientConfig(appConfig, AppClientType.CONSUMER_PRODUCT);
        final Map<String, Object> playerReceiverConfig = extractClientConfig(appConfig, AppClientType.CONSUMER_PLAYER);
        final Map<String, Object> scoreReceiverConfig = extractClientConfig(appConfig, AppClientType.CONSUMER_SCORE);
        final Map<String, Object> scoreCardSenderConfig = extractClientConfig(appConfig, AppClientType.PRODUCER_SCORECARD);

        final AvroPayloadDeserializer<Product> productDeserializer = new AvroPayloadDeserializer<>(productReceiverConfig);
        final AvroPayloadDeserializer<Player> playerDeserializer = new AvroPayloadDeserializer<>(playerReceiverConfig);
        final AvroPayloadDeserializer<ScoreEvent> scoreEventDeserializer = new AvroPayloadDeserializer<>(scoreReceiverConfig);

        /*
         * Subscribe to single partition topic containing product entries
         */
        final ProductStatefulReceiver productReceiver = new ProductStatefulReceiver(
                productReceiverConfig,
                new KafkaConsumer<>(productReceiverConfig, longDeserializer, productDeserializer)
        );
        new Thread(productReceiver).start();

        /*
         * Subscribe to multi-partition topic containing player entries
         */
        final PlayerStatefulReceiver playerReceiver = new PlayerStatefulReceiver(
                playerReceiverConfig,
                new KafkaConsumer<>(playerReceiverConfig, longDeserializer, playerDeserializer)
        );
        new Thread(playerReceiver).start();

        /*
         * Subscribe to multi-partition topic containing score events keyed by playerId
         */
        final ScoreEventReceiver scoreEventReceiver = new ScoreEventReceiver(
                scoreReceiverConfig,
                playerReceiver,
                new KafkaConsumer<>(scoreReceiverConfig, longDeserializer, scoreEventDeserializer)
        );
        new Thread(scoreEventReceiver).start();


        /*
         * Responsible for sending agg.'d scoreCards keyed by playerId
         */
        final ScoreCardSender scoreCardSender = new ScoreCardSender(scoreCardSenderConfig);
        new ScoreEventListener(
                scoreEventReceiver,
                (ProductStateListener) productReceiver.getStateListener(),
                (PlayerStateListener) playerReceiver.getStateListener(),
                scoreCardSender
        );

        onShutdownHooks(
                productDeserializer,
                playerDeserializer,
                scoreEventDeserializer,
                productReceiver,
                playerReceiver,
                scoreEventReceiver,
                scoreCardSender
        );
    }

    private static void onShutdownHooks(AvroPayloadDeserializer<Product> productDeserializer, AvroPayloadDeserializer<Player> playerDeserializer, AvroPayloadDeserializer<ScoreEvent> scoreEventDeserializer, ProductStatefulReceiver productReceiver, PlayerStatefulReceiver playerReceiver, ScoreEventReceiver scoreEventReceiver, ScoreCardSender scoreCardSender) {
        Runtime.getRuntime().addShutdownHook(new Thread(new ConsumerCloser<>(productReceiver)));
        Runtime.getRuntime().addShutdownHook(new Thread(new ConsumerCloser<>(playerReceiver)));
        Runtime.getRuntime().addShutdownHook(new Thread(new ConsumerCloser<>(scoreEventReceiver)));
        Runtime.getRuntime().addShutdownHook(new Thread(scoreCardSender::close));
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> {
                    productDeserializer.close();
                    playerDeserializer.close();
                    scoreEventDeserializer.close();
                }
        ));
    }


    static void loadConfig(final String file, final Properties properties) {
        try (
                final FileInputStream inputStream = new FileInputStream(file)
        ) {
            properties.load(inputStream);
            properties.put("client.id", getClientIdOrDefault());
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    static String getClientIdOrDefault() {
        return System.getenv().getOrDefault("POD_NAME", "stateful-rebalance-client");
    }

    public static Map<String,Object> propertiesToMap(final Properties properties) {
        final Map<String, Object> configs = new HashMap<>();
        properties.forEach(
                (key, value) -> configs.put((String) key, value)
        );
        return configs;
    }


    public static Map<String, Object> extractClientConfig(final Map<String, Object> config, final AppClientType clientType) {
        switch (clientType) {
            case PRODUCER_SCORECARD:
            case CONSUMER_SCORE:
            case CONSUMER_PRODUCT:
            case CONSUMER_PLAYER:
                return ConfigExtractor.extractConfig(config, clientType);
            default:
                log.warn("Application.extractClientConfig() unable to prep config passed for clientType : {}. Return config as-is.", clientType);
                return config;
        }
    }
}
