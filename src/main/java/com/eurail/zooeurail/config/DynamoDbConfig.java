package com.eurail.zooeurail.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;

import java.net.URI;


/**
 * Spring configuration class for DynamoDB client setup.
 * <p>
 * This configuration provides beans for both low-level {@link DynamoDbClient} and
 * enhanced {@link DynamoDbEnhancedClient} operations. It supports flexible credential
 * configuration for different environments:
 * </p>
 * <ul>
 *   <li>Local development with static credentials or DynamoDB Local</li>
 *   <li>Production with AWS Default Credentials Provider chain (IAM roles, environment variables, etc.)</li>
 *   <li>Custom endpoint override for local testing</li>
 * </ul>
 * <p>
 * Configuration properties:
 * </p>
 * <ul>
 *   <li>{@code app.dynamodb.endpoint} - Custom endpoint URL (e.g., for DynamoDB Local)</li>
 *   <li>{@code app.dynamodb.region} - AWS region for the client</li>
 *   <li>{@code app.dynamodb.access-key} - Explicit AWS access key (optional)</li>
 *   <li>{@code app.dynamodb.secret-key} - Explicit AWS secret key (optional)</li>
 *   <li>{@code app.dynamodb.use-default-credentials} - Whether to use AWS default credentials chain</li>
 * </ul>
 *
 * @see DynamoDbClient
 * @see DynamoDbEnhancedClient
 */
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
     * Optional explicit AWS credentials (for local/testing). If not provided and
     * {@code app.dynamodb.use-default-credentials=true}, the default provider chain will be used
     * (recommended for production: IAM role, environment, etc.).
     */
    @Value("${app.dynamodb.access-key:}")
    private String accessKey;

    /**
     * AWS secret key for explicit credential configuration.
     */
    @Value("${app.dynamodb.secret-key:}")
    private String secretKey;

    /**
     * Flag indicating whether to use the AWS Default Credentials Provider chain.
     * When {@code true}, the client will use IAM roles, environment variables, or
     * other standard AWS credential sources. Defaults to {@code false}.
     */
    @Value("${app.dynamodb.use-default-credentials:false}")
    private boolean useDefaultCredentials;

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
                .credentialsProvider(resolveCredentialsProvider());

        // Apply endpoint override only if provided (e.g., DynamoDB Local)
        if (endpoint != null && !endpoint.isBlank()) {
            builder = builder.endpointOverride(URI.create(endpoint));
        }
        DynamoDbClient client = builder.build();
        // Touch client early to fail-fast on misconfiguration
        client.listTables(ListTablesRequest.builder().limit(1).build());
        return client;
    }

    /**
     * Resolves the appropriate AWS credentials provider based on configuration.
     * <p>
     * The resolution logic follows this priority:
     * </p>
     * <ol>
     *   <li>If {@code useDefaultCredentials} is {@code true}, use {@link DefaultCredentialsProvider}
     *       which follows the AWS default credential provider chain</li>
     *   <li>If explicit {@code accessKey} and {@code secretKey} are provided, use static credentials</li>
     *   <li>Fallback to local development credentials ("local"/"local") for convenience</li>
     * </ol>
     *
     * @return the configured {@link AwsCredentialsProvider}
     */
    private AwsCredentialsProvider resolveCredentialsProvider() {
        if (useDefaultCredentials) {
            // Use the AWS Default Credentials Provider chain (best practice for prod)
            return DefaultCredentialsProvider.create();
        }
        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        }
        // Fallback for local development convenience
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("local", "local"));
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