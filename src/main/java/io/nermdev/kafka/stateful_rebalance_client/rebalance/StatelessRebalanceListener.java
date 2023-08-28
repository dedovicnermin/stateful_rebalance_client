package io.nermdev.kafka.stateful_rebalance_client.rebalance;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class StatelessRebalanceListener implements ConsumerRebalanceListener {
    private static final Logger log = LoggerFactory.getLogger(StatelessRebalanceListener.class);
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> collection) {
        log.info("ON_PARTITIONS_REVOKED : {}", collection);
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> collection) {
        log.info("ON_P_ASSIGNED invoked : {}", collection);
    }

    @Override
    public void onPartitionsLost(Collection<TopicPartition> partitions) {
        log.info("ON PARTITIONS LOST INVOKED FOR THE FOLLOWING PARTITIONS : {}", partitions);
    }
}
