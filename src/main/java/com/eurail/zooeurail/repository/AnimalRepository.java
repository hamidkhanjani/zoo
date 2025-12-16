package com.eurail.zooeurail.repository;

import com.eurail.zooeurail.model.Animal;

import java.util.List;

public interface AnimalRepository extends BaseRepository<Animal> {
    List<Animal> findAll();

    /** Find animals by exact title using GSI (no scan). */
    List<Animal> findByTitle(String title);

    /** Find animals by room id using GSI (no scan). */
    List<Animal> findByRoomId(String roomId);

    /**
     * Find up to the first N animals by room id using GSI (no scan), leveraging DynamoDB pagination under the hood.
     * This method will only fetch as many pages as needed to accumulate the requested limit.
     */
    List<Animal> findByRoomIdFirstN(String roomId, int limit);
}
