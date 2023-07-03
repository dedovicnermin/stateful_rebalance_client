package io.nermdev.kafka.stateful_rebalance_client.util;

import io.nermdev.kafka.stateful_rebalance_client.Application;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ConfigExtractorTest {

    @Test
    void testExtractPlayers() {
        final Properties method = method();
        final Map<String, Object> stringObjectMap = Application.propertiesToMap(method);


        final Map<String, Object> actual = ConfigExtractor.extractConfig(stringObjectMap, AppClientType.CONSUMER,"player");
        Assertions.assertThat(actual).isNotNull();
        actual.keySet().stream().sorted()
                .forEach(key -> {
                    System.out.println(key + " : " + actual.get(key));
                });




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