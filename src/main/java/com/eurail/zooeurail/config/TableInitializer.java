package com.eurail.zooeurail.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;


/**
 * Component responsible for initializing DynamoDB tables on application startup.
 * <p>
 * This component is active only in "dev" and "test" profiles and when the property
 * {@code aws.dynamodb.createTablesOnStartup} is set to {@code true} (default).
 * <p>
 * It ensures that the required DynamoDB tables ("animals" and "rooms") exist,
 * creating them if they are not found. The "animals" table includes Global Secondary Indexes
 * for querying by title and roomId.
 */
@Component
@RequiredArgsConstructor
@Profile({"dev", "test"})
@ConditionalOnProperty(name = "aws.dynamodb.createTablesOnStartup", havingValue = "true", matchIfMissing = true)
public class TableInitializer {
    private static final Logger log = LoggerFactory.getLogger(TableInitializer.class);

    private final DynamoDbClient dynamoDbClient;

    /**
     * Initializes the required DynamoDB tables on application startup.
     * <p>
     * This method is automatically invoked after dependency injection is complete.
     * It ensures that the "animals" and "rooms" tables exist in DynamoDB.
     */
    @PostConstruct
    public void init() {
        ensureTable("animals");
        ensureTable("rooms");
    }

    /**
     * Ensures that a DynamoDB table exists, creating it if necessary.
     * <p>
     * For the "animals" table, this method creates Global Secondary Indexes on "title" and "roomId".
     * For other tables (e.g., "rooms"), it creates a simple table with only a primary key.
     *
     * @param tableName the name of the table to ensure exists
     */
    private void ensureTable(String tableName) {
        try {
            dynamoDbClient.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
            log.info("DynamoDB table '{}' already exists", tableName);
        } catch (ResourceNotFoundException rnfe) {
            log.info("Creating DynamoDB table '{}'...", tableName);
            CreateTableRequest create;
            if ("animals".equals(tableName)) {
                create = CreateTableRequest.builder()
                        .tableName(tableName)
                        .billingMode(BillingMode.PAY_PER_REQUEST)
                        .keySchema(KeySchemaElement.builder()
                                .attributeName("id")
                                .keyType(KeyType.HASH)
                                .build())
                        .attributeDefinitions(
                                AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build(),
                                AttributeDefinition.builder().attributeName("title").attributeType(ScalarAttributeType.S).build(),
                                AttributeDefinition.builder().attributeName("roomId").attributeType(ScalarAttributeType.S).build()
                        )
                        .globalSecondaryIndexes(
                                GlobalSecondaryIndex.builder()
                                        .indexName("gsi_title")
                                        .keySchema(KeySchemaElement.builder().attributeName("title").keyType(KeyType.HASH).build())
                                        .projection(p -> p.projectionType(ProjectionType.ALL))
                                        .build(),
                                GlobalSecondaryIndex.builder()
                                        .indexName("gsi_roomId")
                                        .keySchema(KeySchemaElement.builder().attributeName("roomId").keyType(KeyType.HASH).build())
                                        .projection(p -> p.projectionType(ProjectionType.ALL))
                                        .build()
                        )
                        .build();
            } else {
                create = CreateTableRequest.builder()
                        .tableName(tableName)
                        .billingMode(BillingMode.PAY_PER_REQUEST)
                        .keySchema(KeySchemaElement.builder()
                                .attributeName("id")
                                .keyType(KeyType.HASH)
                                .build())
                        .attributeDefinitions(AttributeDefinition.builder()
                                .attributeName("id")
                                .attributeType(ScalarAttributeType.S)
                                .build())
                        .build();
            }
            dynamoDbClient.createTable(create);
            log.info("DynamoDB table '{}' created", tableName);
        }
    }
}