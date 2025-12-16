package com.eurail.zooeurail.repository.dynamodb;

import com.eurail.zooeurail.model.Room;
import com.eurail.zooeurail.repository.RoomRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

import java.util.Optional;


public class RoomDynamoDbRepository implements RoomRepository {

    private final DynamoDbRepository<Room> delegate;

    public RoomDynamoDbRepository(DynamoDbEnhancedClient client, String tableName) {
        this.delegate = DynamoDbRepository.of(client, Room.class, tableName);
    }

    @Override
    public Room save(Room entity) {
        return delegate.save(entity);
    }

    @Override
    public Optional<Room> findById(String id) {
        return delegate.findById(id);
    }

    @Override
    public void deleteById(String id) {
        delegate.deleteById(id);
    }
}
