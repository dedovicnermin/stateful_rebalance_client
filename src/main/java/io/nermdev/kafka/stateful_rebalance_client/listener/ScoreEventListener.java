package io.nermdev.kafka.stateful_rebalance_client.listener;


import io.nermdev.kafka.stateful_rebalance_client.listener.state.PlayerStateListener;
import io.nermdev.kafka.stateful_rebalance_client.listener.state.ProductStateListener;
import io.nermdev.kafka.stateful_rebalance_client.receiver.ReceiveEvent;
import io.nermdev.kafka.stateful_rebalance_client.receiver.leaderboard.ScoreEventReceiver;
import io.nermdev.kafka.stateful_rebalance_client.sender.ScoreCardSender;
import io.nermdev.schemas.avro.leaderboards.Player;
import io.nermdev.schemas.avro.leaderboards.Product;
import io.nermdev.schemas.avro.leaderboards.ScoreCard;
import io.nermdev.schemas.avro.leaderboards.ScoreEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ScoreEventListener implements EventListener<Long, ScoreEvent> {
    private static final Logger log = LoggerFactory.getLogger(ScoreEventListener.class);
    private final ProductStateListener productState;
    private final PlayerStateListener playerState;
    private final ScoreCardSender scoreCardSender;

    public ScoreEventListener(final ScoreEventReceiver receiver, final ProductStateListener productState, final PlayerStateListener playerState, final Map<String, Object> configs) {
        receiver.addListener(this);
        this.playerState = playerState;
        this.productState = productState;
        this.scoreCardSender = new ScoreCardSender(Collections.unmodifiableMap(configs));

    }


    @Override
    public void onEvent(ReceiveEvent<Long, ScoreEvent> event) {
        final ScoreCard scoreCard = Optional.ofNullable(event.getPayload())
                .map(payload -> {
                    final Optional<Player> player = playerState.getValue(payload.getPlayerId());
                    final Optional<Product> product = productState.getValue(payload.getProductId());
                    return new ScoreCard(player.orElse(null), product.orElse(null), payload.getScore(), payload.getDate());
                }).orElse(new ScoreCard());

        if (Objects.isNull(scoreCard.getPlayer()) || Objects.isNull(scoreCard.getProduct())) {
            log.info("%n--- HAD TO ABORT BC MISSING STATE : {} | PRODUCT: {} | PLAYER : {} ---%n", event.getPayload(), scoreCard.getProduct(), scoreCard.getPlayer());
            return;
        }
        scoreCardSender.send(event.getKey(), scoreCard);
    }


}
