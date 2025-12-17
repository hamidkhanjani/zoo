package com.eurail.zooeurail.repository;

import com.eurail.zooeurail.model.Animal;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * Repository interface for managing {@link Animal} entities in the zoo management system.
 * <p>
 * This repository extends {@link BaseRepository} and provides additional query methods
 * optimized for DynamoDB operations, including Global Secondary Index (GSI) queries
 * and aggregation operations on animal data.
 * </p>
 * <p>
 * All query methods are designed to avoid full table scans by leveraging DynamoDB
 * indexes and efficient query patterns.
 * </p>
 *
 * @see Animal
 * @see BaseRepository
 */
public interface AnimalRepository extends BaseRepository<Animal> {
    /**
     * Retrieves all animals from the repository.
     * <p>
     * Note: This operation may perform a full table scan in DynamoDB and should
     * be used cautiously with large datasets.
     * </p>
     *
     * @return a list of all animals, or an empty list if none exist
     */
    List<Animal> findAll();

    /**
     * Finds animals by their exact title using a Global Secondary Index (GSI).
     * <p>
     * This method performs an efficient query operation without scanning the entire table.
     * </p>
     *
     * @param title the exact title to search for
     * @return a list of animals matching the specified title, or an empty list if none found
     */
    List<Animal> findByTitle(String title);

    /**
     * Finds animals located in a specific room using a Global Secondary Index (GSI).
     * <p>
     * This method performs an efficient query operation without scanning the entire table.
     * </p>
     *
     * @param roomId the ID of the room to search for
     * @return a list of animals in the specified room, or an empty list if none found
     */
    List<Animal> findByRoomId(String roomId);

    /**
     * Finds up to the first N animals in a specific room using GSI with pagination support.
     * <p>
     * This method leverages DynamoDB pagination to fetch only the necessary pages to
     * accumulate the requested number of results, optimizing performance and reducing
     * data transfer costs.
     * </p>
     *
     * @param roomId the ID of the room to search for
     * @param limit  the maximum number of animals to retrieve
     * @return a list of up to {@code limit} animals in the specified room, or an empty list if none found
     */
    List<Animal> findByRoomIdFirstN(String roomId, int limit);

    /**
     * Aggregates the count of animals that have marked each room as a favorite.
     * <p>
     * This method uses DynamoDB features to efficiently aggregate favorite room counts
     * across all animals. Implementations should use projection expressions to fetch only
     * the {@code favoriteRoomIds} field when possible, avoiding retrieval of full items.
     * </p>
     * <p>
     * Only room IDs present in the {@code roomIdsUniverse} collection are included in
     * the aggregation results.
     * </p>
     *
     * @param roomIdsUniverse a collection of room IDs to filter the aggregation; only these IDs will be counted
     * @return a map where keys are room IDs and values are the count of animals that favorited each room
     */
    Map<String, Long> aggregateFavoriteRoomCounts(Collection<String> roomIdsUniverse);

    /**
     * Aggregates the count of animals that have marked each room as a favorite.
     * <p>
     * This method uses DynamoDB features to efficiently aggregate favorite room counts
     * across all animals without filtering by a specific set of room IDs. Implementations
     * should use projection expressions to fetch only the {@code favoriteRoomIds} field
     * when possible, avoiding retrieval of full items.
     * </p>
     *
     * @return a map where keys are room IDs and values are the count of animals that favorited each room
     */
    Map<String, Long> aggregateFavoriteRoomCounts();
}