package io.nermdev.kafka.stateful_rebalance_client.receiver.leaderboard;

import io.nermdev.kafka.stateful_rebalance_client.framework.AbstractContainerBaseTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

class ProductStatefulReceiverTest extends AbstractContainerBaseTest {


    @Test
    void test() {

        System.out.println(getKafkaContainer().getBootstrapServers());
    }
}