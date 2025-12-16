package com.eurail.zooeurail.repository;

import com.eurail.zooeurail.model.Animal;

import java.util.List;

public interface AnimalRepository extends BaseRepository<Animal> {
    List<Animal> findAll();

    /** Find animals by exact title using GSI (no scan). */
    List<Animal> findByTitle(String title);

    /** Find animals by room id using GSI (no scan). */
    List<Animal> findByRoomId(String roomId);
}
