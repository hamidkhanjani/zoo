package com.eurail.zooeurail.service;

import com.eurail.zooeurail.exception.ServiceException;
import com.eurail.zooeurail.model.BaseEntity;
import com.eurail.zooeurail.repository.BaseRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Optional;

/**
 * Base service class providing common CRUD operations for entities.
 * <p>
 * This service acts as an intermediary between the controller layer and the repository layer,
 * providing business logic, error handling, and automatic timestamp management for entity
 * operations. All service exceptions are wrapped in {@link ServiceException} for consistent
 * error handling.
 * </p>
 * <p>
 * The service automatically manages the {@code created} and {@code updated} timestamps
 * of entities to ensure consistent audit trails across all operations.
 * </p>
 *
 * @param <T> the entity type extending {@link BaseEntity} managed by this service
 * @see BaseEntity
 * @see BaseRepository
 * @see ServiceException
 */
@Slf4j
public class BaseService<T extends BaseEntity> {

    /**
     * The repository instance used for data access operations.
     */
    protected final BaseRepository<T> repository;

    /**
     * Constructs a new BaseService with the specified repository.
     *
     * @param repository the repository instance to use for data access operations; must not be {@code null}
     */
    public BaseService(BaseRepository<T> repository) {
        this.repository = repository;
    }

    /**
     * Creates a new entity in the repository.
     * <p>
     * This method automatically sets the creation timestamp to the current time
     * and ensures the {@code updated} timestamp is not set on creation.
     * </p>
     *
     * @param entity the entity to create; must not be {@code null}
     * @return the created entity with populated timestamps and identifier
     * @throws ServiceException if the creation operation fails
     */
    public T create(T entity) {
        try {
            // Set creation timestamp only; do not set updated on create
            entity.setCreated(Instant.now());
            entity.setUpdated(null);
            T saved = repository.save(entity);
            if (log.isInfoEnabled()) log.info("Created entity id={}", saved.getId());
            return saved;
        } catch (RuntimeException e) {
            throw new ServiceException("Failed to create entity", e);
        }
    }

    /**
     * Retrieves an entity by its unique identifier.
     * <p>
     * If the entity is not found, a warning is logged and an empty {@link Optional} is returned.
     * </p>
     *
     * @param id the unique identifier of the entity to retrieve; must not be {@code null}
     * @return an {@link Optional} containing the entity if found, or {@link Optional#empty()} if not found
     * @throws ServiceException if the retrieval operation fails
     */
    public Optional<T> get(String id) {
        try {
            Optional<T> found = repository.findById(id);
            if (found.isEmpty() && log.isWarnEnabled()) log.warn("Entity not found id={}", id);
            return found;
        } catch (RuntimeException e) {
            throw new ServiceException("Failed to get entity by id: " + id, e);
        }
    }

    /**
     * Updates an existing entity in the repository.
     * <p>
     * This method ensures that:
     * <ul>
     *   <li>The entity ID matches the provided path parameter</li>
     *   <li>The original {@code created} timestamp is preserved</li>
     *   <li>The {@code updated} timestamp is set to the current time</li>
     * </ul>
     * </p>
     *
     * @param id      the unique identifier of the entity to update; must not be {@code null}
     * @param updated the entity with updated fields; must not be {@code null}
     * @return the updated entity with refreshed timestamps
     * @throws ServiceException if the entity does not exist or the update operation fails
     */
    public T update(String id, T updated) {
        try {
            // Ensure the entity ID matches the path param
            updated.setId(id);

            // Preserve original created timestamp if the entity exists
            repository.findById(id).ifPresentOrElse(
                    existing -> updated.setCreated(existing.getCreated()),
                    () -> {
                        throw new ServiceException("Entity not found for update id=" + id);
                    }
            );
            updated.setUpdated(Instant.now());

            // Update the 'updated' timestamp on every modification
            updated.setUpdated(Instant.now());

            T saved = repository.save(updated);
            if (log.isInfoEnabled()) log.info("Updated entity id={}", id);
            return saved;
        } catch (RuntimeException e) {
            throw new ServiceException("Failed to update entity id=" + id, e);
        }
    }

    /**
     * Deletes an entity by its unique identifier.
     * <p>
     * This method completes silently if the entity does not exist.
     * </p>
     *
     * @param id the unique identifier of the entity to delete; must not be {@code null}
     * @throws ServiceException if the deletion operation fails
     */
    public void delete(String id) {
        try {
            repository.deleteById(id);
            if (log.isInfoEnabled()) log.info("Deleted entity id={}", id);
        } catch (RuntimeException e) {
            throw new ServiceException("Failed to delete entity id=" + id, e);
        }
    }
}