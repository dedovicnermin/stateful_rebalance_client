package io.nermdev.kafka.stateful_rebalance_client.receiver.leaderboard;


import io.nermdev.kafka.stateful_rebalance_client.model.PayloadOrError;
import io.nermdev.kafka.stateful_rebalance_client.rebalance.SleepyRebalanceListener;
import io.nermdev.kafka.stateful_rebalance_client.receiver.BaseReceiver;
import io.nermdev.kafka.stateful_rebalance_client.receiver.ReceiveEvent;
import io.nermdev.kafka.stateful_rebalance_client.util.LeaderboardUtils;
import io.nermdev.schemas.avro.leaderboards.ScoreEvent;
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
import java.util.stream.Collectors;

public class ScoreEventReceiver extends BaseReceiver<Long, ScoreEvent> {
    public static final String LEADERBOARD_SCORES_TOPIC = "leaderboard.scores";
    private static final Logger log = LoggerFactory.getLogger(ScoreEventReceiver.class);


    private final  KafkaConsumer<Long, PayloadOrError<ScoreEvent>> consumer;
    private final PlayerStatefulReceiver playerReceiver;


    public ScoreEventReceiver(
            final Map<String, Object> config,
            final PlayerStatefulReceiver playerReceiver,
            final KafkaConsumer<Long, PayloadOrError<ScoreEvent>> consumer
    ) {
        super(config);
        this.consumer = consumer;
        this.playerReceiver = playerReceiver;
        LeaderboardUtils.configureForK8(consumerConfig, "score");
    }

    @Override
    protected String getTopicName() {
        return LEADERBOARD_SCORES_TOPIC;
    }


    @Override
    protected KafkaConsumer<Long, PayloadOrError<ScoreEvent>> getConsumer() {
        return consumer;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }


    @Override
    public void start() {
        run();
    }

    @Override
    public void close() {
        consumer.wakeup();
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            log.error(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        log.info("Receiver and consumer closed.");
    }



    @SneakyThrows
    @Override
    public void run() {
        consumerConfig.put("client.id", "consumer-scores" + System.getenv("POD_NAME"));
        log.debug("Subscribing to topic : {}", topic);
        consumer.subscribe(Collections.singleton(topic), new SleepyRebalanceListener());
        while (playerReceiver.getStateListener().getAppState().size() == 0) {
            log.debug("Confirmed player state is empty. Waiting...");
            log.info("Waiting for ");
            Thread.sleep(1000L);
        }
        try {
            while (true) {
                final ConsumerRecords<Long, PayloadOrError<ScoreEvent>> consumerRecords = consumer.poll(pollDuration);
                if (consumerRecords.isEmpty()) continue;
                if (failsCoPartitionRequirement(consumer.assignment())) {
                    log.debug("CoPartition requirement failed. Enforcing a re-balance now...");
                    consumer.enforceRebalance();
                    continue;
                }

                for (ConsumerRecord<Long, PayloadOrError<ScoreEvent>> cr : consumerRecords) {
                    final ReceiveEvent<Long, ScoreEvent> scoreReceiveEvent = LeaderboardUtils.createReceiveEvent(cr);
                    log.debug("Preparing to fire : {}", scoreReceiveEvent);
                    fire(scoreReceiveEvent);
                }
                consumer.commitAsync();
            }
        } catch (WakeupException e) {
            log.info("Consumer poll woke up");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Something unexpected happened in scoreEventReceiver : {}", e.getMessage());
        } finally {
            try {
                consumer.commitSync();
            } finally {
                log.info("Gracefully closing consumer...");
                consumer.close();
                countDownLatch.countDown();
                log.info("The scoreevent consuemr is now gracefully closed");
            }
//            avroPayloadDeserializer.close();

        }
    }


    private boolean failsCoPartitionRequirement(final Set<TopicPartition> currAssignment) {
        final Set<Integer> playerPartitions = playerReceiver.getCurrAssignment().stream()
                .map(TopicPartition::partition)
                .collect(Collectors.toSet());
        final Set<Integer> scorePartitions = currAssignment.stream()
                .map(TopicPartition::partition)
                .collect(Collectors.toSet());
        return !playerPartitions.equals(scorePartitions);
    }
}
