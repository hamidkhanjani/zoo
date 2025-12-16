package com.eurail.zooeurail.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
public abstract class DynamoDbTestBase {

    @Container
    private static final GenericContainer<?> dynamodb = new GenericContainer<>("amazon/dynamodb-local:latest")
            .withExposedPorts(8000)
            .withCommand("-jar", "DynamoDBLocal.jar", "-inMemory", "-sharedDb");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        if (dynamodb != null && dynamodb.isRunning()) {
            registry.add("app.dynamodb.endpoint", () -> "http://" + dynamodb.getHost() + ":" + dynamodb.getMappedPort(8000));
        }
        registry.add("app.dynamodb.region", () -> "us-west-2");
    }
}
