package com.eurail.zooeurail.service;

import com.eurail.zooeurail.model.BaseEntity;
import com.eurail.zooeurail.repository.BaseRepository;

import java.time.Instant;
import java.util.Optional;


public class BaseService<T extends BaseEntity> {

    protected final BaseRepository<T> repository;

    public BaseService(BaseRepository<T> repository) {
        this.repository = repository;
    }

    public T create(T entity) {
        // Set creation timestamp only; do not set updated on create
        entity.setCreated(Instant.now());
        entity.setUpdated(null);
        return repository.save(entity);
    }

    public Optional<T> get(String id) {
        return repository.findById(id);
    }

    public T update(String id, T updated) {
        // Ensure the entity ID matches the path param
        updated.setId(id);

        // Preserve original created timestamp if the entity exists
        repository.findById(id).ifPresent(existing -> updated.setCreated(existing.getCreated()));

        // Update the 'updated' timestamp on every modification
        updated.setUpdated(Instant.now());

        return repository.save(updated);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }
}
