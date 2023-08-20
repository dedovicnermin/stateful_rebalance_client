package io.nermdev.kafka.stateful_rebalance_client.util;

import io.nermdev.kafka.stateful_rebalance_client.Application;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.io.InputStream;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;


class ConfigExtractorTest {

    Properties appConfig;

    @ParameterizedTest
    @EnumSource(value = AppClientType.class, names = {"CONSUMER_PLAYER", "CONSUMER_SCORE", "CONSUMER_PRODUCT", "PRODUCER_SCORECARD"})
    void testExtractPlayers(final AppClientType clientType) {
        System.out.println(clientType.name());
        appConfig = Optional.ofNullable(appConfig)
                .orElse(method());
        final Map<String, Object> stringObjectMap = Application.propertiesToMap(appConfig);

        final Map<String, Object> actual = ConfigExtractor.extractConfig(stringObjectMap, clientType);
        Assertions.assertThat(actual).isNotNull();
        actual.keySet().stream().sorted()
                .forEach(key -> System.out.println(key + " : " + actual.get(key)));
        final String suffix = clientType.name().toLowerCase().split("_")[1];
        Assertions.assertThat((String)actual.get("client.id")).isNotNull().contains(suffix);
    }

    private Properties method() {
        final Properties properties = new Properties();
        try (
                final InputStream inputStream = ConfigExtractorTest.class.getClassLoader().getResourceAsStream("configextractor.properties")
        ) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties;
    }

}