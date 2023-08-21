package io.nermdev.kafka.stateful_rebalance_client.listener.state;


import io.nermdev.kafka.stateful_rebalance_client.receiver.EventReceiver;
import io.nermdev.kafka.stateful_rebalance_client.receiver.ReceiveEvent;
import io.nermdev.kafka.stateful_rebalance_client.util.LeaderboardUtils;
import io.nermdev.schemas.avro.leaderboards.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ProductStateListener extends StateEventListener<Long, Product> {
    private static final Logger log = LoggerFactory.getLogger(ProductStateListener.class);

    public ProductStateListener(final EventReceiver<Long, Product> receiver) {
        super();
        receiver.addListener(this);
    }

    @Override
    public void onEvent(ReceiveEvent<Long, Product> event) {
        log.debug("Listened to event : {}", event.getPayload());
        Optional.ofNullable(event.getPayload())
                .ifPresent(
                        product -> put(
                                LeaderboardUtils.tpOf(event.getRecord().topic(), event.getRecord().partition()),
                                event.getKey(),
                                event.getPayload()
                        )
                );
    }

    public void printState() {
        printState("PRODUCT_STATE");
    }
}
