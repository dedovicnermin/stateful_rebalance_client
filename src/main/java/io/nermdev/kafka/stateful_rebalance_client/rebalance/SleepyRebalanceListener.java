package io.nermdev.kafka.stateful_rebalance_client.rebalance;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class SleepyRebalanceListener implements ConsumerRebalanceListener {
    private static final Logger log = LoggerFactory.getLogger(SleepyRebalanceListener.class);
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> collection) {
        log.info("SleepyRebalanceListener: ON_PARTITIONS_REVOKED : {}", collection);
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> collection) {
        log.info("SleepyRebalanceListener: ON_P_ASSIGNED invoked : {}", collection);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.info("ERROR IN SLEEPY REBALANCER ON partitions assigned cb : {}", e.getMessage());
//            throw new RuntimeException(e);
        }
    }

    @Override
    public void onPartitionsLost(Collection<TopicPartition> partitions) {
        log.info("SLEEPY REBALANCER : ON PARTITIONS LOST INVOKED FOR THE FOLLOWING PARTITIONS : {}", partitions);
    }
}
