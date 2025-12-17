package com.eurail.zooeurail.repository.dynamodb;

import com.eurail.zooeurail.exception.RepositoryException;
import com.eurail.zooeurail.repository.BaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;

/**
 * Generic DynamoDB repository implementation providing CRUD operations for entities.
 * <p>
 * This class implements the {@link BaseRepository} interface and provides a concrete
 * implementation for interacting with AWS DynamoDB using the AWS SDK Enhanced Client.
 * It handles common database operations including saving, retrieving, and deleting entities.
 * </p>
 * <p>
 * The repository uses DynamoDB's enhanced client features and provides comprehensive
 * error handling with logging. All operations throw {@link RepositoryException} when
 * runtime errors occur during database interactions.
 * </p>
 *
 * @param <T> the type of entity this repository manages
 * @see BaseRepository
 * @see RepositoryException
 */
@Slf4j
@RequiredArgsConstructor
public class DynamoDbRepository<T> implements BaseRepository<T> {

    /**
     * The DynamoDB table instance used for database operations.
     */
    private final DynamoDbTable<T> table;

    /**
     * Factory method to create a DynamoDbRepository instance.
     * <p>
     * Creates a repository by initializing a DynamoDB table reference using the
     * enhanced client, entity class, and table name. The table schema is automatically
     * derived from the provided bean class.
     * </p>
     *
     * @param <T>       the type of entity the repository will manage
     * @param client    the DynamoDB enhanced client instance
     * @param clazz     the class type of the entity
     * @param tableName the name of the DynamoDB table
     * @return a new DynamoDbRepository instance configured for the specified entity type and table
     */
    public static <T> DynamoDbRepository<T> of(DynamoDbEnhancedClient client, Class<T> clazz, String tableName) {
        DynamoDbTable<T> table = client.table(tableName, TableSchema.fromBean(clazz));
        return new DynamoDbRepository<>(table);
    }

    /**
     * Saves an entity to the DynamoDB table.
     * <p>
     * Persists the provided entity to DynamoDB. If an entity with the same key already exists,
     * it will be overwritten. The operation is logged at debug level upon successful completion.
     * </p>
     *
     * @param entity the entity to save
     * @return the saved entity
     * @throws RepositoryException if a runtime error occurs during the save operation
     */
    @Override
    public T save(T entity) {
        try {
            table.putItem(entity);
            if (log.isDebugEnabled()) log.debug("Saved entity: {}", entity);
            return entity;
        } catch (RuntimeException e) {
            log.error("Failed to save entity {}", entity, e);
            throw new RepositoryException("Failed to save entity", e);
        }
    }

    /**
     * Retrieves an entity by its partition key ID.
     * <p>
     * Queries DynamoDB for an entity matching the specified ID. If no entity is found,
     * an empty Optional is returned. The operation is logged at debug level.
     * </p>
     *
     * @param id the partition key value identifying the entity
     * @return an Optional containing the entity if found, or empty if not found
     * @throws RepositoryException if a runtime error occurs during the retrieval operation
     */
    @Override
    public Optional<T> findById(String id) {
        try {
            T item = table.getItem(r -> r.key(k -> k.partitionValue(id)));
            if (item == null) {
                if (log.isDebugEnabled()) log.debug("Entity not found by id: {}", id);
                return Optional.empty();
            }
            if (log.isDebugEnabled()) log.debug("Found entity by id {}: {}", id, item);
            return Optional.of(item);
        } catch (RuntimeException e) {
            log.error("Failed to find entity by id {}", id, e);
            throw new RepositoryException("Failed to find entity by id: " + id, e);
        }
    }

    /**
     * Deletes an entity by its partition key ID.
     * <p>
     * Removes the entity with the specified ID from DynamoDB. If no entity with the given ID
     * exists, the operation completes without error. The operation is logged at debug level.
     * </p>
     *
     * @param id the partition key value identifying the entity to delete
     * @throws RepositoryException if a runtime error occurs during the delete operation
     */
    @Override
    public void deleteById(String id) {
        try {
            table.deleteItem(r -> r.key(k -> k.partitionValue(id)));
            if (log.isDebugEnabled()) log.debug("Deleted entity by id: {}", id);
        } catch (RuntimeException e) {
            log.error("Failed to delete entity by id {}", id, e);
            throw new RepositoryException("Failed to delete entity by id: " + id, e);
        }
    }
}