package com.eurail.zooeurail.repository;

import java.util.Optional;

/**
 * Base repository interface providing common CRUD operations for entities.
 * <p>
 * This interface defines the fundamental data access methods that all repository
 * implementations should provide. It follows a generic pattern where {@code T}
 * represents the entity type being managed.
 * </p>
 * <p>
 * All entities managed by implementations of this interface are expected to have
 * a String-based unique identifier.
 * </p>
 *
 * @param <T> the entity type managed by this repository
 */
public interface BaseRepository<T> {
    /**
     * Saves the given entity.
     * <p>
     * If the entity does not exist, it will be created. If it already exists
     * (based on its identifier), it will be updated.
     * </p>
     *
     * @param entity the entity to save; must not be {@code null}
     * @return the saved entity, never {@code null}
     */
    T save(T entity);

    /**
     * Retrieves an entity by its unique identifier.
     *
     * @param id the unique identifier of the entity to retrieve; must not be {@code null}
     * @return an {@link Optional} containing the entity if found, or {@link Optional#empty()} if not found
     */
    Optional<T> findById(String id);

    /**
     * Deletes the entity with the given identifier.
     * <p>
     * If no entity with the given identifier exists, this method may complete
     * silently without throwing an exception, depending on the implementation.
     * </p>
     *
     * @param id the unique identifier of the entity to delete; must not be {@code null}
     */
    void deleteById(String id);
}