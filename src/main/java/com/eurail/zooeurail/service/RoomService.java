package com.eurail.zooeurail.service;

import com.eurail.zooeurail.model.Room;
import com.eurail.zooeurail.repository.RoomRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.Optional;

/**
 * Service class for managing {@link Room} entities with caching support.
 * <p>
 * This service extends {@link BaseService} and adds caching capabilities for room operations
 * to improve performance by reducing database queries. All read operations leverage Spring's
 * caching abstraction to store results, while write operations ensure cache consistency by
 * evicting stale entries.
 * </p>
 * <p>
 * Cache configuration:
 * <ul>
 *   <li>Cache name: {@code roomsById}</li>
 *   <li>Cache key: The room's unique identifier</li>
 *   <li>Eviction strategy: Write operations (create, update, delete) automatically invalidate cached entries</li>
 * </ul>
 * </p>
 *
 * @see Room
 * @see RoomRepository
 * @see BaseService
 */
public class RoomService extends BaseService<Room> {
    /**
     * Constructs a new RoomService with the specified repository.
     *
     * @param repository the room repository instance to use for data access operations; must not be {@code null}
     */
    public RoomService(RoomRepository repository) {
        super(repository);
    }

    /**
     * Retrieves a room by its unique identifier with caching support.
     * <p>
     * If the room is found in the cache, it will be returned without querying the database.
     * Otherwise, the database will be queried and the result will be cached for subsequent requests.
     * </p>
     *
     * @param id the unique identifier of the room to retrieve; must not be {@code null}
     * @return an {@link Optional} containing the room if found, or {@link Optional#empty()} if not found
     * @throws com.eurail.zooeurail.exception.ServiceException if the retrieval operation fails
     */
    @Override
    @Cacheable(cacheNames = "roomsById", key = "#id")
    public Optional<Room> get(String id) {
        return super.get(id);
    }

    /**
     * Creates a new room in the repository and evicts the cache entry.
     * <p>
     * After successful creation, the cache entry for the newly created room is evicted
     * to ensure subsequent queries retrieve the freshest data from the database.
     * Cache eviction only occurs if the creation was successful (result is not null).
     * </p>
     *
     * @param entity the room to create; must not be {@code null}
     * @return the created room with populated timestamps and identifier
     * @throws com.eurail.zooeurail.exception.ServiceException if the creation operation fails
     */
    @Override
    @CacheEvict(cacheNames = {"roomsById"}, key = "#result.id", condition = "#result != null")
    public Room create(Room entity) {
        return super.create(entity);
    }

    /**
     * Updates an existing room in the repository and evicts the cache entry.
     * <p>
     * After successful update, the cache entry for the specified room ID is evicted
     * to ensure subsequent queries retrieve the updated data from the database.
     * </p>
     *
     * @param id      the unique identifier of the room to update; must not be {@code null}
     * @param updated the room with updated fields; must not be {@code null}
     * @return the updated room with refreshed timestamps
     * @throws com.eurail.zooeurail.exception.ServiceException if the room does not exist or the update operation fails
     */
    @Override
    @CacheEvict(cacheNames = {"roomsById"}, key = "#id")
    public Room update(String id, Room updated) {
        return super.update(id, updated);
    }

    /**
     * Deletes a room by its unique identifier and evicts the cache entry.
     * <p>
     * After successful deletion, the cache entry for the specified room ID is evicted
     * to ensure the cache remains consistent with the database state.
     * </p>
     *
     * @param id the unique identifier of the room to delete; must not be {@code null}
     * @throws com.eurail.zooeurail.exception.ServiceException if the deletion operation fails
     */
    @Override
    @CacheEvict(cacheNames = {"roomsById"}, key = "#id")
    public void delete(String id) {
        super.delete(id);
    }
}