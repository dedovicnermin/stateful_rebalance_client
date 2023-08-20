package io.nermdev.kafka.stateful_rebalance_client.util;

public enum AppClientType {
    PRODUCER_SCORECARD("producers.scorecard"),
    CONSUMER_PRODUCT("consumers.product"),
    CONSUMER_PLAYER("consumers.player"),
    CONSUMER_SCORE("consumers.score");
    private final String prefix;
    AppClientType(String clientConfigPrefix) {
        this.prefix = clientConfigPrefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
