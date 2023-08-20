package io.nermdev.kafka.stateful_rebalance_client.receiver.leaderboard;


import io.nermdev.kafka.stateful_rebalance_client.model.PayloadOrError;
import io.nermdev.kafka.stateful_rebalance_client.rebalance.SleepyRebalanceListener;
import io.nermdev.kafka.stateful_rebalance_client.receiver.BaseReceiver;
import io.nermdev.kafka.stateful_rebalance_client.receiver.ReceiveEvent;
import io.nermdev.kafka.stateful_rebalance_client.serializer.AvroPayloadDeserializer;
import io.nermdev.kafka.stateful_rebalance_client.util.AppClientType;
import io.nermdev.kafka.stateful_rebalance_client.util.LeaderboardUtils;
import io.nermdev.schemas.avro.leaderboards.ScoreEvent;
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
import java.util.stream.Collectors;

public class ScoreEventReceiver extends BaseReceiver<Long, ScoreEvent> {
    private static final Logger log = LoggerFactory.getLogger(ScoreEventReceiver.class);


    private final  KafkaConsumer<Long, PayloadOrError<ScoreEvent>> consumer;
    private final PlayerStatefulReceiver playerReceiver;
    private final Duration duration;

    public ScoreEventReceiver(final Map<String, Object> config, final PlayerStatefulReceiver playerReceiver, final KafkaConsumer<Long, PayloadOrError<ScoreEvent>> consumer) {
        this(config, playerReceiver, consumer, Duration.ofMillis(500));
    }


    public ScoreEventReceiver(
            final Map<String, Object> config,
            final PlayerStatefulReceiver playerReceiver,
            final KafkaConsumer<Long, PayloadOrError<ScoreEvent>> consumer,
            final Duration duration
    ) {
        super(config);
//        this.avroDeserializer = new AvroPayloadDeserializer<>(consumerConfig);
        this.consumer = consumer;
        this.playerReceiver = playerReceiver;
        this.duration = duration;
        LeaderboardUtils.configureForK8(consumerConfig, "score");
    }

    @Override
    protected String getTopicName() {
        return "leaderboard.scores";
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
//        consumer = new KafkaConsumer<>(consumerConfig, longDeserializer, avroPayloadDeserializer);
        consumer.subscribe(Collections.singleton(topic), new SleepyRebalanceListener());
        final Duration pto = Duration.ofMillis(500);

        try {
            while (true) {
                final ConsumerRecords<Long, PayloadOrError<ScoreEvent>> consumerRecords = consumer.poll(pto);
                if (consumerRecords.isEmpty()) continue;
                if (failsCoPartitionRequirement(consumer.assignment())) consumer.enforceRebalance();
                for (ConsumerRecord<Long, PayloadOrError<ScoreEvent>> cr : consumerRecords) {
                    final ReceiveEvent<Long, ScoreEvent> scoreReceiveEvent = LeaderboardUtils.createReceiveEvent(cr);
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
