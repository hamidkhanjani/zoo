package com.eurail.zooeurail.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;

import java.net.URI;


@Configuration
public class DynamoDbConfig {

    /**
     * DynamoDB endpoint URL, defaults to local DynamoDB instance.
     */
    @Value("${app.dynamodb.endpoint:http://localhost:8000}")
    private String endpoint;

    /**
     * AWS region for DynamoDB client configuration.
     */
    @Value("${app.dynamodb.region:us-west-2}")
    private String region;

    /**
     * Creates and configures a DynamoDB client bean for low-level operations.
     * <p>
     * The client is configured with:
     * <ul>
     *   <li>Static credentials for local development</li>
     *   <li>Custom endpoint override for DynamoDB Local</li>
     *   <li>Configurable region</li>
     * </ul>
     * <p>
     * On creation, the client performs a test request to fail fast if misconfigured.
     *
     * @return configured DynamoDB client instance
     */
    @Bean
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("local", "local")))
                .endpointOverride(URI.create(endpoint));
        DynamoDbClient client = builder.build();
        // Touch client early to fail-fast on misconfiguration
        client.listTables(ListTablesRequest.builder().limit(1).build());
        return client;
    }

    /**
     * Creates a DynamoDbEnhancedClient bean for enhanced DynamoDB operations.
     * <p>
     * The enhanced client provides higher-level abstractions and object mapping
     * capabilities for working with DynamoDB.
     *
     * @param client the low-level DynamoDB client to wrap
     * @return configured DynamoDbEnhancedClient instance
     */
    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    }
}