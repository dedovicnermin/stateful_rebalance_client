package io.nermdev.kafka.stateful_rebalance_client.framework;

import net.christophschubert.cp.testcontainers.CPTestContainerFactory;
import net.christophschubert.cp.testcontainers.SchemaRegistryContainer;
import net.christophschubert.cp.testcontainers.util.TestContainerUtils;
import org.junit.jupiter.api.AfterAll;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class AbstractContainerBaseTest {
    private static final CPTestContainerFactory FACTORY = new CPTestContainerFactory();
    private static KafkaContainer KAFKA_CONTAINER;
    private static SchemaRegistryContainer SR_CONTAINER;

    static void createKafkaContainer() {
        KAFKA_CONTAINER = FACTORY.createKafka();
        SR_CONTAINER = FACTORY.createSchemaRegistry(KAFKA_CONTAINER);

//        final DockerImageName imageName = DockerImageName.parse("confluentinc/cp-kafka:7.3.3");
//        return new KafkaContainer(imageName)
//                .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1") //transaction.state.log.replication.factor
//                .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1") //transaction.state.log.min.isr
//                .withEnv("KAFKA_TRANSACTION_STATE_LOG_NUM_PARTITIONS", "1") //transaction.state.log.num.partitions
//                .withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0")
//                .withReuse(true);
    }

    static {
        createKafkaContainer();
        SR_CONTAINER.start();
        TestContainerUtils.prettyPrintEnvs(KAFKA_CONTAINER);
        TestContainerUtils.prettyPrintEnvs(SR_CONTAINER);
    }

    public static KafkaContainer getKafkaContainer() {
        return KAFKA_CONTAINER;
    }
    public static SchemaRegistryContainer getSrContainer() {
        return SR_CONTAINER;
    }

    @AfterAll
    static void close() {
        KAFKA_CONTAINER.close();
        SR_CONTAINER.close();
    }
}
