package com.eurail.zooeurail.config;

import com.eurail.zooeurail.model.Animal;
import com.eurail.zooeurail.model.Room;
import com.eurail.zooeurail.repository.AnimalRepository;
import com.eurail.zooeurail.repository.RoomRepository;
import com.eurail.zooeurail.service.AnimalService;
import com.eurail.zooeurail.service.RoomService;
import com.eurail.zooeurail.repository.dynamodb.AnimalDynamoDbRepository;
import com.eurail.zooeurail.repository.dynamodb.RoomDynamoDbRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/**
 * Spring configuration class for repository and service beans.
 * Configures DynamoDB tables, repositories, and services for the Zoo application.
 */
@Configuration
public class RepositoryConfig {

    /**
     * Optional prefix to prepend to all DynamoDB table names.
     * Configured via {@code app.dynamodb.table-prefix} property (defaults to empty string).
     */
    @Value("${app.dynamodb.table-prefix:}")
    private String tablePrefix;

    /**
     * Name of the DynamoDB table for animals.
     * Configured via {@code app.dynamodb.tables.animals} property (defaults to "animals").
     */
    @Value("${app.dynamodb.tables.animals:animals}")
    private String animalsTable;

    /**
     * Name of the DynamoDB table for rooms.
     * Configured via {@code app.dynamodb.tables.rooms} property (defaults to "rooms").
     */
    @Value("${app.dynamodb.tables.rooms:rooms}")
    private String roomsTable;

    /**
     * Name of the Global Secondary Index (GSI) for querying animals by title.
     * Configured via {@code app.dynamodb.indexes.animals.title} property (defaults to "gsi_title").
     */
    @Value("${app.dynamodb.indexes.animals.title:gsi_title}")
    private String animalsTitleIndex;

    /**
     * Name of the Global Secondary Index (GSI) for querying animals by room ID.
     * Configured via {@code app.dynamodb.indexes.animals.room-id} property (defaults to "gsi_roomId").
     */
    @Value("${app.dynamodb.indexes.animals.room-id:gsi_roomId}")
    private String animalsRoomIdIndex;

    /**
     * Whether to use consistent reads for DynamoDB operations.
     * Configured via {@code app.dynamodb.consistent-read} property (defaults to false).
     */
    @Value("${app.dynamodb.consistent-read:false}")
    private boolean consistentRead;

    /**
     * Creates a DynamoDB table bean for Animal entities.
     *
     * @param client the DynamoDB Enhanced Client
     * @return configured DynamoDbTable for Animal entities
     */
    @Bean
    public DynamoDbTable<Animal> animalTable(DynamoDbEnhancedClient client) {
        return client.table(tablePrefix + animalsTable, TableSchema.fromBean(Animal.class));
    }

    /**
     * Creates a DynamoDB table bean for Room entities.
     *
     * @param client the DynamoDB Enhanced Client
     * @return configured DynamoDbTable for Room entities
     */
    @Bean
    public DynamoDbTable<Room> roomTable(DynamoDbEnhancedClient client) {
        return client.table(tablePrefix + roomsTable, TableSchema.fromBean(Room.class));
    }

    /**
     * Creates an AnimalRepository bean using DynamoDB implementation.
     *
     * @param table the configured Animal DynamoDB table
     * @return AnimalRepository instance configured with GSI names and consistent read settings
     */
    @Bean
    public AnimalRepository animalRepository(DynamoDbTable<Animal> table) {
        return new AnimalDynamoDbRepository(table, animalsTitleIndex, animalsRoomIdIndex, consistentRead);
    }

    /**
     * Creates a RoomRepository bean using DynamoDB implementation.
     *
     * @param client the DynamoDB Enhanced Client
     * @return RoomRepository instance configured with the rooms table name
     */
    @Bean
    public RoomRepository roomRepository(DynamoDbEnhancedClient client) {
        return new RoomDynamoDbRepository(client, tablePrefix + roomsTable);
    }

    /**
     * Creates an AnimalService bean.
     *
     * @param repo        the AnimalRepository dependency
     * @param roomService the RoomService dependency
     * @return AnimalService instance
     */
    @Bean
    public AnimalService animalService(AnimalRepository repo, RoomService roomService) {
        return new AnimalService(repo, roomService);
    }

    /**
     * Creates a RoomService bean.
     *
     * @param repo the RoomRepository dependency
     * @return RoomService instance
     */
    @Bean
    public RoomService roomService(RoomRepository repo) {
        return new RoomService(repo);
    }


}