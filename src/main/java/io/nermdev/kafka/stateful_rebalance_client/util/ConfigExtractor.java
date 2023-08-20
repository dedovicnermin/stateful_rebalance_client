package io.nermdev.kafka.stateful_rebalance_client.util;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ConfigExtractor {
    static final String INTERCEPTOR_CLASSES = "interceptor.classes";

    private ConfigExtractor() {}

    public static Map<String, Object> extractConfig(final Map<String, Object> config, final AppClientType type) {
        Map<String, Object> clientConfig = new HashMap<>();
        List<Map.Entry<String, Object>> targetClientConfig = extractClientConfig(config, type);
        targetClientConfig.forEach(
                entry -> clientConfig.put(entry.getKey().substring(type.getPrefix().length() + 1), entry.getValue())
        );
        return configureInterceptors(clientConfig);
    }

    private static Map<String, Object> configureInterceptors(final Map<String, Object> filteredConfig) {
        final Object value = filteredConfig.get(INTERCEPTOR_CLASSES);
        final List<? extends Class<?>> classes = Optional.ofNullable(value)
                .map(v -> (String) v)
                .map(stringClasses -> Arrays.stream(stringClasses.split(",")).collect(Collectors.toList()))
                .map(stringClasses -> stringClasses.stream().map(ConfigExtractor::classForName).collect(Collectors.toList()))
                .orElseGet(ArrayList::new);
        return Optional.of(classes)
                .filter(cs -> !cs.isEmpty())
                .map(cs -> {
                    filteredConfig.put(INTERCEPTOR_CLASSES, cs);
                    return filteredConfig;
                }).orElse(filteredConfig);
    }

    private static Class<?> classForName(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }

    }

    private static List<Map.Entry<String, Object>> extractClientConfig(final Map<String, Object> config, final AppClientType type) {
        return config.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(type.getPrefix()))
                .collect(Collectors.toList());
    }

}
