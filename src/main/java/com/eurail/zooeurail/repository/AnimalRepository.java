package com.eurail.zooeurail.repository;

import com.eurail.zooeurail.model.Animal;

import java.util.List;
import java.util.Map;
import java.util.Collection;

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

    /**
     * Aggregates favorite room counts across all animals using DynamoDB features.
     * Implementations should avoid fetching full items when possible (e.g., projection on favoriteRoomIds only).
     * If {@code roomIdsUniverse} is provided, only those ids are counted.
     */
    Map<String, Long> aggregateFavoriteRoomCounts(Collection<String> roomIdsUniverse);

    /**
     * Aggregates favorite room counts across all animals without filtering by a universe set.
     */
    Map<String, Long> aggregateFavoriteRoomCounts();
}
