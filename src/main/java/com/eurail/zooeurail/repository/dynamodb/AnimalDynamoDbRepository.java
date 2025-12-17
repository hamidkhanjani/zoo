package com.eurail.zooeurail.repository.dynamodb;

import com.eurail.zooeurail.model.Animal;
import com.eurail.zooeurail.repository.AnimalRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.*;

/**
 * DynamoDB implementation of the {@link AnimalRepository} interface.
 * <p>
 * This repository provides data access operations for {@link Animal} entities
 * stored in DynamoDB. It uses an AWS SDK Enhanced DynamoDB client for type-safe
 * operations and supports both primary key access and Global Secondary Index (GSI)
 * queries for efficient data retrieval.
 * </p>
 * <p>
 * Key features include:
 * <ul>
 *   <li>Primary key operations (save, findById, deleteById)</li>
 *   <li>GSI queries by title and room ID</li>
 *   <li>Paginated queries with limit support</li>
 *   <li>Aggregation of favorite room counts with optional filtering</li>
 *   <li>Configurable consistent read behavior</li>
 * </ul>
 * </p>
 *
 * @see AnimalRepository
 * @see Animal
 */
public class AnimalDynamoDbRepository implements AnimalRepository {
    private final DynamoDbTable<Animal> table;
    private final String titleIndexName;
    private final String roomIdIndexName;
    private final boolean consistentRead;

    /**
     * Constructs a new AnimalDynamoDbRepository.
     *
     * @param table           the DynamoDB table instance for Animal entities
     * @param titleIndexName  the name of the GSI for querying by title
     * @param roomIdIndexName the name of the GSI for querying by room ID
     * @param consistentRead  whether to use consistent reads for scan operations
     */
    public AnimalDynamoDbRepository(DynamoDbTable<Animal> table,
                                    String titleIndexName,
                                    String roomIdIndexName,
                                    boolean consistentRead) {
        this.table = table;
        this.titleIndexName = titleIndexName;
        this.roomIdIndexName = roomIdIndexName;
        this.consistentRead = consistentRead;
    }

    /**
     * Saves an animal entity to DynamoDB.
     * <p>
     * This operation will create a new item or replace an existing item
     * with the same primary key.
     * </p>
     *
     * @param entity the animal entity to save
     * @return the saved animal entity
     */
    @Override
    public Animal save(Animal entity) {
        table.putItem(entity);
        return entity;
    }

    /**
     * Finds an animal by its unique identifier.
     *
     * @param id the unique identifier of the animal
     * @return an {@link Optional} containing the animal if found, or empty if not found
     */
    @Override
    public Optional<Animal> findById(String id) {
        return Optional.ofNullable(table.getItem(r -> r.key(k -> k.partitionValue(id))));
    }

    /**
     * Deletes an animal by its unique identifier.
     *
     * @param id the unique identifier of the animal to delete
     */
    @Override
    public void deleteById(String id) {
        table.deleteItem(r -> r.key(k -> k.partitionValue(id)));
    }

    /**
     * Retrieves all animals from DynamoDB.
     * <p>
     * This operation performs a table scan and respects the configured
     * consistent read setting. Use with caution on large tables as scans
     * are expensive operations.
     * </p>
     *
     * @return a list of all animals in the table
     */
    @Override
    public List<Animal> findAll() {
        var request = ScanEnhancedRequest.builder()
                .consistentRead(consistentRead)
                .build();
        return table.scan(request).items().stream().toList();
    }

    /**
     * Finds animals by exact title using a Global Secondary Index.
     * <p>
     * This method performs an efficient GSI query rather than a table scan.
     * </p>
     *
     * @param title the exact title to search for
     * @return a list of animals matching the specified title
     */
    @Override
    public List<Animal> findByTitle(String title) {
        var pages = table.index(titleIndexName)
                .query(r -> r.queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(title))));
        return pages.stream()
                .flatMap(p -> p.items().stream())
                .toList();
    }

    /**
     * Finds animals by room ID using a Global Secondary Index.
     * <p>
     * This method performs an efficient GSI query rather than a table scan.
     * </p>
     *
     * @param roomId the room ID to search for
     * @return a list of animals located in the specified room
     */
    @Override
    public List<Animal> findByRoomId(String roomId) {
        var pages = table.index(roomIdIndexName)
                .query(r -> r.queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(roomId))));
        return pages.stream()
                .flatMap(p -> p.items().stream())
                .toList();
    }

    /**
     * Finds up to the first N animals by room ID using a Global Secondary Index.
     * <p>
     * This method leverages DynamoDB pagination to fetch only as many pages as
     * needed to accumulate the requested limit, making it more efficient than
     * fetching all results and then limiting.
     * </p>
     *
     * @param roomId the room ID to search for
     * @param limit  the maximum number of animals to return
     * @return a list of up to {@code limit} animals located in the specified room,
     * or an empty list if {@code limit <= 0}
     */
    @Override
    public List<Animal> findByRoomIdFirstN(String roomId, int limit) {
        if (limit <= 0) return java.util.Collections.emptyList();
        var request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(roomId)))
                .limit(limit)
                .build();
        var pages = table.index(roomIdIndexName).query(request);
        // Flatten page items and stop when we reach the requested limit
        return pages.stream()
                .flatMap(p -> p.items().stream())
                .limit(limit)
                .toList();
    }

    /**
     * Aggregates the count of animals that have each room ID in their favorite rooms list.
     * <p>
     * Only room IDs present in the provided universe collection are counted.
     * This method uses projection to fetch only the favoriteRoomIds attribute,
     * minimizing data transfer.
     * </p>
     *
     * @param roomIdsUniverse the collection of room IDs to include in the aggregation
     * @return a map where keys are room IDs and values are the count of animals
     * that have marked that room as a favorite
     */
    @Override
    public Map<String, Long> aggregateFavoriteRoomCounts(Collection<String> roomIdsUniverse) {
        return aggregateFavoriteRoomCountsInternal(roomIdsUniverse);
    }

    /**
     * Aggregates the count of animals that have each room ID in their favorite rooms list.
     * <p>
     * All room IDs are counted without filtering. This method uses projection to
     * fetch only the favoriteRoomIds attribute, minimizing data transfer.
     * </p>
     *
     * @return a map where keys are room IDs and values are the count of animals
     * that have marked that room as a favorite
     */
    @Override
    public Map<String, Long> aggregateFavoriteRoomCounts() {
        return aggregateFavoriteRoomCountsInternal(null);
    }

    /**
     * Internal method to aggregate favorite room counts with optional filtering.
     * <p>
     * Performs a table scan with projection on the favoriteRoomIds attribute only
     * to minimize I/O. If a universe of room IDs is provided, only those IDs are
     * counted; otherwise, all favorite room IDs are included in the aggregation.
     * </p>
     *
     * @param roomIdsUniverse optional collection of room IDs to filter by; if null,
     *                        all room IDs are counted
     * @return a map where keys are room IDs and values are the count of animals
     * that have marked that room as a favorite
     */
    private Map<String, Long> aggregateFavoriteRoomCountsInternal(Collection<String> roomIdsUniverse) {
        // Project only the favoriteRoomIds attribute to minimize IO; consistentRead as configured
        var scanRequest = ScanEnhancedRequest.builder()
                .consistentRead(consistentRead)
                .attributesToProject("favoriteRoomIds")
                .build();

        Set<String> universe = (roomIdsUniverse == null) ? null : new HashSet<>(roomIdsUniverse);
        Map<String, Long> counts = new HashMap<>();

        table.scan(scanRequest).items().forEach(animal -> {
            var favs = animal.getFavoriteRoomIds();
            if (favs == null || favs.isEmpty()) return;
            favs.stream().filter(Objects::nonNull).filter(rid -> universe == null || universe.contains(rid)).forEach(rid -> counts.merge(rid, 1L, Long::sum));
        });

        return counts;
    }
}