package io.nermdev.kafka.stateful_rebalance_client;



import io.nermdev.kafka.stateful_rebalance_client.listener.ScoreEventListener;
import io.nermdev.kafka.stateful_rebalance_client.listener.state.PlayerStateListener;
import io.nermdev.kafka.stateful_rebalance_client.listener.state.ProductStateListener;
import io.nermdev.kafka.stateful_rebalance_client.receiver.leaderboard.PlayerStatefulReceiver;
import io.nermdev.kafka.stateful_rebalance_client.receiver.leaderboard.ProductStatefulReceiver;
import io.nermdev.kafka.stateful_rebalance_client.receiver.leaderboard.ScoreEventReceiver;
import io.nermdev.kafka.stateful_rebalance_client.util.ConsumerCloser;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    @SneakyThrows
    public static void main(String[] args) {
        if (args.length < 1) throw new IllegalArgumentException("Pass path to application.properties");
        final Properties properties = new Properties();
        loadConfig(args[0], properties);
        log.info("PROPERTIES: {}", properties);


        final Map<String, Object> consumerConfig = propertiesToMap(properties);
        final ProductStatefulReceiver productReceiver = new ProductStatefulReceiver(consumerConfig);
        new Thread(productReceiver).start();
        Runtime.getRuntime().addShutdownHook(new Thread(new ConsumerCloser<>(productReceiver)));

        final PlayerStatefulReceiver playerReceiver = new PlayerStatefulReceiver(propertiesToMap(properties));
        new Thread(playerReceiver).start();
        Runtime.getRuntime().addShutdownHook(new Thread(new ConsumerCloser<>(playerReceiver)));


        Thread.sleep(10000);
        final ScoreEventReceiver scoreEventReceiver = new ScoreEventReceiver(propertiesToMap(properties), playerReceiver);
        new Thread(scoreEventReceiver).start();
        Runtime.getRuntime().addShutdownHook(new Thread(new ConsumerCloser<>(scoreEventReceiver)));

        new ScoreEventListener(scoreEventReceiver, (ProductStateListener) productReceiver.getStateListener(), (PlayerStateListener) playerReceiver.getStateListener(), propertiesToMap(properties));

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
}
