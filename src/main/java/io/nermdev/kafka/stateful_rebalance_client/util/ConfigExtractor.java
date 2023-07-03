package io.nermdev.kafka.stateful_rebalance_client.util;


import java.util.*;
import java.util.stream.Collectors;

public final class ConfigExtractor {
    static final String CONSUMER_PREFIX="consumers.";
    static final String PRODUCER_PREFIX="producers.";
    static final String INTERCEPTOR_CLASSES = "interceptor.classes";


    private ConfigExtractor() {}

    public static Map<String, Object> extractConfig(final Map<String, Object> config, final AppClientType clientType, final String clientPrefix) {
        Map<String, Object> clientConfig = new HashMap<>();
        List<Map.Entry<String, Object>> targetClientConfig;
        if (clientType == AppClientType.CONSUMER) {
            targetClientConfig = extractClientConfig(config, CONSUMER_PREFIX, clientPrefix);
        } else {
            targetClientConfig = extractClientConfig(config, PRODUCER_PREFIX, clientPrefix);
        }



        targetClientConfig.forEach(
                entry -> clientConfig.put(entry.getKey().substring(CONSUMER_PREFIX.length() + clientPrefix.length() + 1), entry.getValue())
        );

        Optional.ofNullable((String)clientConfig.get(INTERCEPTOR_CLASSES))
                .ifPresent(s -> {
                    final List<String> collect = Arrays.stream(s.split(",")).collect(Collectors.toList());
                    if (collect.size() > 1) {
                        try {
                            clientConfig.put(INTERCEPTOR_CLASSES, Arrays.asList(Class.forName(collect.get(0)), Class.forName(collect.get(1))));
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        try {
                            clientConfig.put(INTERCEPTOR_CLASSES, Collections.singletonList(Class.forName(collect.get(0))));
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

        return clientConfig;
    }



    private static List<Map.Entry<String, Object>> extractClientConfig(final Map<String, Object> config, final String basePrefix, final String clientPrefix) {
        return config.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(basePrefix + clientPrefix))
                .collect(Collectors.toList());
    }








}
