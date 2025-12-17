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


@Configuration
public class RepositoryConfig {

    // Table names and GSIs externalized with sensible defaults
    @Value("${app.dynamodb.table-prefix:}")
    private String tablePrefix;

    @Value("${app.dynamodb.tables.animals:animals}")
    private String animalsTable;

    @Value("${app.dynamodb.tables.rooms:rooms}")
    private String roomsTable;

    @Value("${app.dynamodb.indexes.animals.title:gsi_title}")
    private String animalsTitleIndex;

    @Value("${app.dynamodb.indexes.animals.room-id:gsi_roomId}")
    private String animalsRoomIdIndex;

    @Value("${app.dynamodb.consistent-read:false}")
    private boolean consistentRead;

    @Bean
    public DynamoDbTable<Animal> animalTable(DynamoDbEnhancedClient client) {
        return client.table(tablePrefix + animalsTable, TableSchema.fromBean(Animal.class));
    }

    @Bean
    public DynamoDbTable<Room> roomTable(DynamoDbEnhancedClient client) {
        return client.table(tablePrefix + roomsTable, TableSchema.fromBean(Room.class));
    }

    @Bean
    public AnimalRepository animalRepository(DynamoDbTable<Animal> table) {
        return new AnimalDynamoDbRepository(table, animalsTitleIndex, animalsRoomIdIndex, consistentRead);
    }

    @Bean
    public RoomRepository roomRepository(DynamoDbEnhancedClient client) {
        return new RoomDynamoDbRepository(client, tablePrefix + roomsTable);
    }

    @Bean
    public AnimalService animalService(AnimalRepository repo, RoomService roomService) {
        return new AnimalService(repo, roomService);
    }

    @Bean
    public RoomService roomService(RoomRepository repo) {
        return new RoomService(repo);
    }

    // Implementation classes moved to dedicated package: com.eurail.zooeurail.repository.dynamodb

}
