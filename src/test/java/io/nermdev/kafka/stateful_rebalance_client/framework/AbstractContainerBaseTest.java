package io.nermdev.kafka.stateful_rebalance_client.framework;

import net.christophschubert.cp.testcontainers.CPTestContainerFactory;
import net.christophschubert.cp.testcontainers.SchemaRegistryContainer;
import net.christophschubert.cp.testcontainers.util.TestContainerUtils;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Testcontainers;


@Testcontainers
public abstract class AbstractContainerBaseTest {
    private static final CPTestContainerFactory FACTORY = new CPTestContainerFactory();
    private static final KafkaContainer KAFKA_CONTAINER;
    private static final SchemaRegistryContainer SR_CONTAINER;


    static {
        KAFKA_CONTAINER = FACTORY.createKafka();
        SR_CONTAINER = FACTORY.createSchemaRegistry(KAFKA_CONTAINER);
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

}
