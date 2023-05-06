package io.nermdev.kafka.stateful_rebalance_client.listener.state;


import io.nermdev.kafka.stateful_rebalance_client.receiver.EventReceiver;
import io.nermdev.kafka.stateful_rebalance_client.receiver.ReceiveEvent;
import io.nermdev.kafka.stateful_rebalance_client.util.LeaderboardUtils;
import io.nermdev.schemas.avro.leaderboards.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class PlayerStateListener extends StateEventListener<Long, Player> {

    private static final Logger log = LoggerFactory.getLogger(PlayerStateListener.class);

    public PlayerStateListener(EventReceiver<Long, Player> receiver) {
        super();
        receiver.addListener(this);
    }


    public void printState() {
        printState("PLAYER_STATE");
    }


    @Override
    public void onEvent(ReceiveEvent<Long, Player> event) {
        log.debug("Received event : {}", event.getPayload());
        Optional.ofNullable(event.getPayload())
                .ifPresent(
                        payload -> put(LeaderboardUtils.tpOf(event.getRecord().topic(), event.getRecord().partition()), event.getKey(), payload)
                );
    }



}
