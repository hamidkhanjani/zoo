package com.eurail.zooeurail.service;

import com.eurail.zooeurail.model.Room;
import com.eurail.zooeurail.repository.RoomRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.Optional;

public class RoomService extends BaseService<Room> {
    public RoomService(RoomRepository repository) {
        super(repository);
    }

    @Override
    @Cacheable(cacheNames = "roomsById", key = "#id")
    public Optional<Room> get(String id) {
        return super.get(id);
    }

    @Override
    @CacheEvict(cacheNames = {"roomsById"}, key = "#result.id", condition = "#result != null")
    public Room create(Room entity) {
        return super.create(entity);
    }

    @Override
    @CacheEvict(cacheNames = {"roomsById"}, key = "#id")
    public Room update(String id, Room updated) {
        return super.update(id, updated);
    }

    @Override
    @CacheEvict(cacheNames = {"roomsById"}, key = "#id")
    public void delete(String id) {
        super.delete(id);
    }
}
