package io.nermdev.kafka.stateful_rebalance_client.receiver.leaderboard;



import io.nermdev.kafka.stateful_rebalance_client.listener.state.PlayerStateListener;
import io.nermdev.kafka.stateful_rebalance_client.listener.state.StateEventListener;
import io.nermdev.kafka.stateful_rebalance_client.model.PayloadOrError;
import io.nermdev.kafka.stateful_rebalance_client.rebalance.StatefulRebalanceListener;
import io.nermdev.kafka.stateful_rebalance_client.receiver.BaseStatefulReceiver;
import io.nermdev.kafka.stateful_rebalance_client.serializer.AvroPayloadDeserializer;
import io.nermdev.kafka.stateful_rebalance_client.util.LeaderboardUtils;
import io.nermdev.schemas.avro.leaderboards.Player;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class PlayerStatefulReceiver extends BaseStatefulReceiver<Long, Player> {
    private static final Logger log = LoggerFactory.getLogger(PlayerStatefulReceiver.class);
    private KafkaConsumer<Long, PayloadOrError<Player>> consumer;
    private final PlayerStateListener stateListener;
    private Set<TopicPartition> currAssignment;

    public PlayerStatefulReceiver(final Map<String, Object> properties) {
        super(properties);
        LeaderboardUtils.configureForK8(consumerConfig, "player");
        this.stateListener = new PlayerStateListener(this);
    }

    public Set<TopicPartition> getCurrAssignment() {
        return Collections.unmodifiableSet(currAssignment);
    }

    @SneakyThrows
    @Override
    public void run() {
        final LongDeserializer longDeserializer = new LongDeserializer();
        final AvroPayloadDeserializer<Player> avroPayloadDeserializer = new AvroPayloadDeserializer<>(consumerConfig);
        consumerConfig.put("client.id", "consumer-player" + System.getenv("POD_NAME"));
        consumer = new KafkaConsumer<>(consumerConfig, longDeserializer, avroPayloadDeserializer);
        final StatefulRebalanceListener<Long, Player> rebalanceListener = new StatefulRebalanceListener<>(consumer, consumerConfig, longDeserializer, stateListener);
        consumer.subscribe(Collections.singleton(topic), rebalanceListener);
        final Duration pto = Duration.ofMillis(500);

        try {
            while (true) {
                final ConsumerRecords<Long, PayloadOrError<Player>> consumerRecords = consumer.poll(pto);
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
            avroPayloadDeserializer.close();
            rebalanceListener.close();
        }

    }


    @Override
    protected String getGroupName(Map<String, Object> config) {
//        return config.get("group.id") + ".player";
        return (String) config.get("group.id");

    }

    @Override
    protected String getTopicName(Map<String, Object> config) {
        return (String) config.getOrDefault("players.topic", "leaderboard.players");
    }

    @Override
    protected KafkaConsumer<Long, PayloadOrError<Player>> getConsumer() {
        return consumer;
    }

    @Override
    public StateEventListener<Long, Player> getStateListener() {
        return stateListener;
    }
}
