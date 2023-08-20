package io.nermdev.kafka.stateful_rebalance_client.framework;

import net.christophschubert.cp.testcontainers.util.TestContainerUtils;
import org.junit.jupiter.api.AfterAll;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class AbstractContainerBaseTest {
    private static final KafkaContainer KAFKA_CONTAINER = createKafkaContainer();

    static KafkaContainer createKafkaContainer() {
        final DockerImageName imageName = DockerImageName.parse("confluentinc/cp-kafka:7.3.3");
        return new KafkaContainer(imageName)
                .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1") //transaction.state.log.replication.factor
                .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1") //transaction.state.log.min.isr
                .withEnv("KAFKA_TRANSACTION_STATE_LOG_NUM_PARTITIONS", "1") //transaction.state.log.num.partitions
                .withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0")
                .withReuse(true);
    }

    static {
        KAFKA_CONTAINER.start();
        TestContainerUtils.prettyPrintEnvs(KAFKA_CONTAINER);
    }

    public KafkaContainer getKafkaContainer() {
        return KAFKA_CONTAINER;
    }

    @AfterAll
    static void close() {
        KAFKA_CONTAINER.close();
    }
}
