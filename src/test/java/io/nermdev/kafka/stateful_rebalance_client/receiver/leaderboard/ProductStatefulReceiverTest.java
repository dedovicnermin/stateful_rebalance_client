package io.nermdev.kafka.stateful_rebalance_client.receiver.leaderboard;

import io.nermdev.kafka.stateful_rebalance_client.framework.AbstractContainerBaseTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

class ProductStatefulReceiverTest extends AbstractContainerBaseTest {


    @Test
    void test() throws InterruptedException {

        System.out.println(getKafkaContainer().getBootstrapServers());
        System.out.printf(getSrContainer().getBaseUrl());
        System.out.printf("sleeping...");
        Thread.sleep(10000);
    }
}