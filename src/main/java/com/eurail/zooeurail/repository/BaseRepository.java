package com.eurail.zooeurail.repository;

import java.util.Optional;


public interface BaseRepository<T> {
    T save(T entity);
    Optional<T> findById(String id);
    void deleteById(String id);
}
