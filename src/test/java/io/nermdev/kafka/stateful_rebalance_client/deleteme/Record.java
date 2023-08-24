package io.nermdev.kafka.stateful_rebalance_client.deleteme;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Record {
    @JsonProperty("key")
    private String key;
    @JsonProperty("value")
    private EdaMessage value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public EdaMessage getValue() {
        return value;
    }

    public void setValue(EdaMessage value) {
        this.value = value;
    }
}
