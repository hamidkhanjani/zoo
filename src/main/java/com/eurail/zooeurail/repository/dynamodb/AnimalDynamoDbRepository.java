package com.eurail.zooeurail.repository.dynamodb;

import com.eurail.zooeurail.model.Animal;
import com.eurail.zooeurail.repository.AnimalRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.*;


public class AnimalDynamoDbRepository implements AnimalRepository {
    private final DynamoDbTable<Animal> table;
    private final String titleIndexName;
    private final String roomIdIndexName;
    private final boolean consistentRead;

    public AnimalDynamoDbRepository(DynamoDbTable<Animal> table,
                                    String titleIndexName,
                                    String roomIdIndexName,
                                    boolean consistentRead) {
        this.table = table;
        this.titleIndexName = titleIndexName;
        this.roomIdIndexName = roomIdIndexName;
        this.consistentRead = consistentRead;
    }

    @Override
    public Animal save(Animal entity) {
        table.putItem(entity);
        return entity;
    }

    @Override
    public Optional<Animal> findById(String id) {
        return Optional.ofNullable(table.getItem(r -> r.key(k -> k.partitionValue(id))));
    }

    @Override
    public void deleteById(String id) {
        table.deleteItem(r -> r.key(k -> k.partitionValue(id)));
    }

    @Override
    public List<Animal> findAll() {
        var request = ScanEnhancedRequest.builder()
                .consistentRead(consistentRead)
                .build();
        return table.scan(request).items().stream().toList();
    }

    @Override
    public List<Animal> findByTitle(String title) {
        var pages = table.index(titleIndexName)
                .query(r -> r.queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(title))));
        return pages.stream()
                .flatMap(p -> p.items().stream())
                .toList();
    }

    @Override
    public List<Animal> findByRoomId(String roomId) {
        var pages = table.index(roomIdIndexName)
                .query(r -> r.queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(roomId))));
        return pages.stream()
                .flatMap(p -> p.items().stream())
                .toList();
    }

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

    @Override
    public Map<String, Long> aggregateFavoriteRoomCounts(Collection<String> roomIdsUniverse) {
        return aggregateFavoriteRoomCountsInternal(roomIdsUniverse);
    }

    @Override
    public Map<String, Long> aggregateFavoriteRoomCounts() {
        return aggregateFavoriteRoomCountsInternal(null);
    }

    private Map<String, Long> aggregateFavoriteRoomCountsInternal(Collection<String> roomIdsUniverse) {
        // Project only the favoriteRoomIds attribute to minimize IO; consistentRead as configured
        var scanRequest = ScanEnhancedRequest.builder()
                .consistentRead(consistentRead)
                .attributesToProject("favoriteRoomIds")
                .build();

        java.util.Set<String> universe = (roomIdsUniverse == null) ? null : new java.util.HashSet<>(roomIdsUniverse);
        java.util.Map<String, Long> counts = new java.util.HashMap<>();

        table.scan(scanRequest).items().forEach(animal -> {
            var favs = animal.getFavoriteRoomIds();
            if (favs == null || favs.isEmpty()) return;
            favs.stream().filter(Objects::nonNull).filter(rid -> universe == null || universe.contains(rid)).forEach(rid -> counts.merge(rid, 1L, Long::sum));
        });

        return counts;
    }
}
