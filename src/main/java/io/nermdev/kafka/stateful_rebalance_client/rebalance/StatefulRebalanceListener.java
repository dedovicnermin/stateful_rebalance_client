package io.nermdev.kafka.stateful_rebalance_client.rebalance;


import io.nermdev.kafka.stateful_rebalance_client.listener.state.StateEventListener;
import io.nermdev.kafka.stateful_rebalance_client.model.PayloadOrError;
import io.nermdev.kafka.stateful_rebalance_client.serializer.AvroPayloadDeserializer;
import io.nermdev.kafka.stateful_rebalance_client.util.LeaderboardUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.Duration;
import java.util.*;

/**
 * For stateful incremental/cooperative rebalances
 * @param <K>
 * @param <V>
 */
public class StatefulRebalanceListener<K, V> implements ConsumerRebalanceListener, Closeable {
    private final Logger log = LoggerFactory.getLogger(StatefulRebalanceListener.class);
    private final Map<TopicPartition, OffsetAndMetadata> currOffsets;
    private final KafkaConsumer<K, PayloadOrError<V>> consumer;

    private final Map<String, Object> freeConsumerConfig;
    private final StateEventListener<K, V> stateListener;
    private Collection<TopicPartition> lastAssignment;
    private final AvroPayloadDeserializer<V> deserializer;
    private final Deserializer<K> keyDeserializer;


    public StatefulRebalanceListener(KafkaConsumer<K, PayloadOrError<V>> consumer, final Map<String, Object> properties, final Deserializer<K> keyDeserializer, final StateEventListener<K, V> stateListener) {
        this.consumer = consumer;
        this.freeConsumerConfig = new HashMap<>();
        properties.keySet().stream()
                .filter(k -> k.startsWith("security") || k.startsWith("ssl") || k.startsWith("bootstrap") || k.startsWith("schema") || k.startsWith("sasl"))
                .forEach(key -> freeConsumerConfig.put(key, properties.get(key)));
        freeConsumerConfig.remove(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG);

        this.stateListener = stateListener;
        this.currOffsets = new HashMap<>();
        this.lastAssignment = List.of();
        deserializer = new AvroPayloadDeserializer<>(properties);
        this.keyDeserializer = keyDeserializer;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> collection) {
        log.info("onPartitionsRevoked() called. Committing consumed offsets for the following partitions (Revoked): {}", collection);
        for (final TopicPartition tp : collection) {
            log.info("onPartitionsRevoked callback triggered. Committing offsets: {}", currOffsets);
            if (Objects.isNull(currOffsets.get(tp))) return;
            consumer.commitSync(Map.of(tp,currOffsets.get(tp)));        // commitOffsets(partition)
        }
    }

    // will only include newly added partitions
    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> collection) {
        log.info("onPartitionsAssigned callback triggered. Cleaning up state / initState / initOffset ( Assigned ) : {}", collection);
        if (collection.isEmpty()) return;
        final Set<TopicPartition> assignment = consumer.assignment();
        log.info("onPartitionsAssigned - result of consumer.assignment() : {} \t compared to collection passed into method : {}", assignment, collection);
        cleanupState(assignment);
        initState(assignment);
        initOffset(assignment);
        log.info("onPartitionAssigned callback complete (end Assigned ). LAST_ASSIGNMENT: {} \t NEW_ASSIGNMENT: {} ", lastAssignment, assignment);
        this.lastAssignment = assignment;

    }




    @Override
    public void onPartitionsLost(Collection<TopicPartition> partitions) {
        log.info("ON_PARTITIONS_LOST callback invoked : {}. Cannot do much besides log", partitions);
    }


    public void addOffsetsToTrack(final String topic, int partition, long offset) {
        currOffsets.put(
                new TopicPartition(topic, partition),
                new OffsetAndMetadata(offset+1, null)
        );
    }
    public Map<TopicPartition, OffsetAndMetadata> getCurrOffsets() {
        return currOffsets;
    }


    private void initOffset(Collection<TopicPartition> assignment) {
        for (final TopicPartition tp : assignment) {
            addOffsetsToTrack(tp.topic(), tp.partition(), consumer.position(tp));
        }
    }

    private void initState(Collection<TopicPartition> assignment) {
        final ArrayList<TopicPartition> partitions = new ArrayList<>(CollectionUtils.disjunction(assignment, lastAssignment));
        try (final KafkaConsumer<K, PayloadOrError<V>> freeConsumer = new KafkaConsumer<>(this.freeConsumerConfig, keyDeserializer, deserializer)) {
            final Map<TopicPartition, Long> topicPartitionLongMap = freeConsumer.endOffsets(partitions);
            for (var entry : topicPartitionLongMap.entrySet()) {
                freeConsumer.assign(Set.of(entry.getKey()));
                freeConsumer.seekToBeginning(Set.of(entry.getKey()));
                long freeConsumerPosition = freeConsumer.position(entry.getKey());
                boolean continueReading = true;
                while (continueReading) {
                    final ConsumerRecords<K, PayloadOrError<V>> records = freeConsumer.poll(Duration.ofSeconds(1));
                    for (var rec : records) {
                        stateListener.onEvent(LeaderboardUtils.createReceiveEvent(rec));
                        freeConsumerPosition += 1;
                        if (freeConsumerPosition >= entry.getValue()) {
                            continueReading = false;
                            break;
                        }
                    }
                }
            }
        }
    }



    private void cleanupState(Collection<TopicPartition> assignment) {
        for (TopicPartition tp : new ArrayList<>(CollectionUtils.disjunction(lastAssignment, assignment))) {
            stateListener.clearState(tp);
        }
    }

    @Override
    public void close() {
        log.info("Closing rebalance listener...");
        deserializer.close();
    }
}
