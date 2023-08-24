package io.nermdev.kafka.stateful_rebalance_client.receiver.leaderboard;



import io.nermdev.kafka.stateful_rebalance_client.Application;
import io.nermdev.kafka.stateful_rebalance_client.listener.state.PlayerStateListener;
import io.nermdev.kafka.stateful_rebalance_client.listener.state.StateEventListener;
import io.nermdev.kafka.stateful_rebalance_client.model.PayloadOrError;
import io.nermdev.kafka.stateful_rebalance_client.rebalance.StatefulRebalanceListener;
import io.nermdev.kafka.stateful_rebalance_client.receiver.BaseStatefulReceiver;
import io.nermdev.kafka.stateful_rebalance_client.util.LeaderboardUtils;
import io.nermdev.schemas.avro.leaderboards.Player;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class PlayerStatefulReceiver extends BaseStatefulReceiver<Long, Player> {
    private static final Logger log = LoggerFactory.getLogger(PlayerStatefulReceiver.class);
    public static final String PLAYERS_TOPIC = "leaderboard.players";
    private final KafkaConsumer<Long, PayloadOrError<Player>> consumer;
    private final PlayerStateListener stateListener;
    private Set<TopicPartition> currAssignment;

    public PlayerStatefulReceiver(final Map<String, Object> properties, final KafkaConsumer<Long, PayloadOrError<Player>> consumer) {
        super(properties);
        this.consumer = consumer;
        this.stateListener = new PlayerStateListener(this);
    }

    public Set<TopicPartition> getCurrAssignment() {
        return Collections.unmodifiableSet(currAssignment);
    }

    @SneakyThrows
    @Override
    public void run() {

        consumerConfig.put("client.id", consumerConfig.get("client.id") + "-" + System.getenv("POD_NAME"));
        final StatefulRebalanceListener<Long, Player> rebalanceListener = new StatefulRebalanceListener<>(consumer, consumerConfig, Application.longDeserializer, stateListener);
        consumer.subscribe(Collections.singleton(topic), rebalanceListener);

        try {
            while (true) {
                final ConsumerRecords<Long, PayloadOrError<Player>> consumerRecords = consumer.poll(pollDuration);
                currAssignment = consumer.assignment();
                for (ConsumerRecord<Long, PayloadOrError<Player>> cr : consumerRecords) {
                    fire(LeaderboardUtils.createReceiveEvent(cr));
                    rebalanceListener.addOffsetsToTrack(cr.topic(), cr.partition(), cr.offset());
                }
                consumer.commitAsync();
            }
        } catch (WakeupException e) {
            log.info("Consumer poll woke up");
        } catch (Exception e) {
            log.error("Something unexpected happened : {}", e.getMessage());
        } finally {
            try {
                consumer.commitSync(rebalanceListener.getCurrOffsets());
            } finally {
                log.info("Gracefully closing consumer...");
                consumer.close();
                countDownLatch.countDown();
                log.info("The consumer is now gracefully closed");
            }
            rebalanceListener.close();
        }

    }


    @Override
    protected String getTopicName() {
        return PLAYERS_TOPIC;
    }

    @Override
    protected KafkaConsumer<Long, PayloadOrError<Player>> getConsumer() {
        return consumer;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    public StateEventListener<Long, Player> getStateListener() {
        return stateListener;
    }
}
