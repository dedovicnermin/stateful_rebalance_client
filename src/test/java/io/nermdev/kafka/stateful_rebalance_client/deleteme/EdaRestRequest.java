package io.nermdev.kafka.stateful_rebalance_client.deleteme;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class EdaRestRequest {

    Map<String, Object> keyEntry;
    Map<String, Object> valueEntry;

    public EdaRestRequest withKey(final String key) {
        if (Objects.isNull(key)) return this;
        keyEntry = Map.of(
                "type", "STRING",
                "data", key
        );
        return this;
    }

    public <K> EdaRestRequest withKeyAndType(final K key, final String type) {
        if (Objects.isNull(key) || Objects.isNull(type)) return this;
        keyEntry = Map.of(
            "type", type,
            "data", key
        );
        return this;
    }

    public <V> EdaRestRequest withValue(final V value) {
        if (Objects.isNull(value)) return this;
        valueEntry = Map.of(
                "type", "JSON",
                "data", value
        );
        return this;
    }

    public Map<String, Map<String, Object>> build() {
        Map<String, Map<String, Object>> body = new HashMap<>();
        Optional.ofNullable(keyEntry)
                .ifPresent(keyEntry -> body.put("key", keyEntry));
        Optional.ofNullable(valueEntry)
                .ifPresent(valueEntry -> body.put("value", valueEntry));
        return body;
    }



    @JsonValue
    public Map<String, Map<String, Object>> getBody() {
        return build();
    }
}
