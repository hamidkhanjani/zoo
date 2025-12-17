package com.eurail.zooeurail.repository.dynamodb;

import com.eurail.zooeurail.model.Room;
import com.eurail.zooeurail.repository.RoomRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

import java.util.Optional;

/**
 * DynamoDB implementation of the {@link RoomRepository} interface.
 * <p>
 * This repository provides persistence operations for {@link Room} entities in AWS DynamoDB.
 * It acts as a delegate wrapper around the generic {@link DynamoDbRepository}, providing
 * type-specific repository functionality for Room entities.
 * </p>
 * <p>
 * The repository supports standard CRUD operations including saving, retrieving by ID,
 * and deleting Room entities. All operations are delegated to the underlying
 * {@link DynamoDbRepository} instance.
 * </p>
 *
 * @see RoomRepository
 * @see DynamoDbRepository
 * @see Room
 */
public class RoomDynamoDbRepository implements RoomRepository {

    /**
     * The underlying generic DynamoDB repository that handles actual database operations.
     */
    private final DynamoDbRepository<Room> delegate;

    /**
     * Constructs a new RoomDynamoDbRepository instance.
     * <p>
     * Initializes the repository by creating a {@link DynamoDbRepository} delegate
     * configured for the Room entity type and specified DynamoDB table.
     * </p>
     *
     * @param client    the DynamoDB enhanced client instance used for database operations
     * @param tableName the name of the DynamoDB table where Room entities are stored
     */
    public RoomDynamoDbRepository(DynamoDbEnhancedClient client, String tableName) {
        this.delegate = DynamoDbRepository.of(client, Room.class, tableName);
    }

    /**
     * Saves a Room entity to the DynamoDB table.
     * <p>
     * Persists the provided Room entity. If a room with the same ID already exists,
     * it will be overwritten with the new data.
     * </p>
     *
     * @param entity the Room entity to save
     * @return the saved Room entity
     * @throws com.eurail.zooeurail.exception.RepositoryException if a runtime error occurs during the save operation
     */
    @Override
    public Room save(Room entity) {
        return delegate.save(entity);
    }

    /**
     * Retrieves a Room entity by its unique identifier.
     * <p>
     * Queries DynamoDB for a Room with the specified ID. If no matching room is found,
     * an empty Optional is returned.
     * </p>
     *
     * @param id the unique identifier of the Room to retrieve
     * @return an Optional containing the Room if found, or empty if not found
     * @throws com.eurail.zooeurail.exception.RepositoryException if a runtime error occurs during the retrieval operation
     */
    @Override
    public Optional<Room> findById(String id) {
        return delegate.findById(id);
    }

    /**
     * Deletes a Room entity by its unique identifier.
     * <p>
     * Removes the Room with the specified ID from DynamoDB. If no room with the given ID
     * exists, the operation completes without error.
     * </p>
     *
     * @param id the unique identifier of the Room to delete
     * @throws com.eurail.zooeurail.exception.RepositoryException if a runtime error occurs during the delete operation
     */
    @Override
    public void deleteById(String id) {
        delegate.deleteById(id);
    }
}