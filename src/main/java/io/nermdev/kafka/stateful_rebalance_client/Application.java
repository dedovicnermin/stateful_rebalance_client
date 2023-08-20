package io.nermdev.kafka.stateful_rebalance_client;



import io.nermdev.kafka.stateful_rebalance_client.listener.ScoreEventListener;
import io.nermdev.kafka.stateful_rebalance_client.listener.state.PlayerStateListener;
import io.nermdev.kafka.stateful_rebalance_client.listener.state.ProductStateListener;
import io.nermdev.kafka.stateful_rebalance_client.model.PayloadOrError;
import io.nermdev.kafka.stateful_rebalance_client.receiver.leaderboard.PlayerStatefulReceiver;
import io.nermdev.kafka.stateful_rebalance_client.receiver.leaderboard.ProductStatefulReceiver;
import io.nermdev.kafka.stateful_rebalance_client.receiver.leaderboard.ScoreEventReceiver;
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
        log.info("PROPERTIES: {}", properties);



        final Map<String, Object> consumerConfig = propertiesToMap(properties);
        final Map<String, Object> productReceiverConfig = extractClientConfig(consumerConfig, AppClientType.CONSUMER_PRODUCT);
        final Map<String, Object> playerReceiverConfig = extractClientConfig(consumerConfig, AppClientType.CONSUMER_PLAYER);
        final Map<String, Object> scoreReceiverConfig = extractClientConfig(consumerConfig, AppClientType.CONSUMER_SCORE);



        final AvroPayloadDeserializer<Product> productDeserializer = new AvroPayloadDeserializer<>(productReceiverConfig);
        final AvroPayloadDeserializer<Player> playerDeserializer = new AvroPayloadDeserializer<>(playerReceiverConfig);
        final AvroPayloadDeserializer<ScoreEvent> scoreEventDeserializer = new AvroPayloadDeserializer<>(scoreReceiverConfig);




        final ProductStatefulReceiver productReceiver = new ProductStatefulReceiver(productReceiverConfig, new KafkaConsumer<>(productReceiverConfig, longDeserializer, productDeserializer));
        new Thread(productReceiver).start();
        Runtime.getRuntime().addShutdownHook(new Thread(new ConsumerCloser<>(productReceiver)));

        final PlayerStatefulReceiver playerReceiver = new PlayerStatefulReceiver(playerReceiverConfig, new KafkaConsumer<>(playerReceiverConfig, longDeserializer, playerDeserializer));
        new Thread(playerReceiver).start();
        Runtime.getRuntime().addShutdownHook(new Thread(new ConsumerCloser<>(playerReceiver)));


//        Thread.sleep(10000);
//        final ScoreEventReceiver scoreEventReceiver = new ScoreEventReceiver(propertiesToMap(properties), playerReceiver);
//        new Thread(scoreEventReceiver).start();
//        Runtime.getRuntime().addShutdownHook(new Thread(new ConsumerCloser<>(scoreEventReceiver)));

//        new ScoreEventListener(scoreEventReceiver, (ProductStateListener) productReceiver.getStateListener(), (PlayerStateListener) playerReceiver.getStateListener(), propertiesToMap(properties));

    }


    static void loadConfig(final String file, final Properties properties) {
        try (
                final FileInputStream inputStream = new FileInputStream(file)
        ) {
            properties.load(inputStream);
            properties.put("client.id", getClientIdOrDefault("stateful-rebalance-client"));
        } catch (IOException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    static String getClientIdOrDefault(final String defaultValue) {
        return System.getenv().getOrDefault("POD_NAME", defaultValue);
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
//    public static <V> KafkaConsumer<Long, PayloadOrError<V>> consumerInstance(final Map<String, Object> config) {
//        final AvroPayloadDeserializer<V> payloadDeserializer = new AvroPayloadDeserializer<>(config);
//        return new KafkaConsumer<>(config, longDeserializer, payloadDeserializer);
//    }
}
