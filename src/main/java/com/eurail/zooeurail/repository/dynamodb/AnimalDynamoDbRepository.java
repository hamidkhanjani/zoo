package com.eurail.zooeurail.repository.dynamodb;

import com.eurail.zooeurail.model.Animal;
import com.eurail.zooeurail.repository.AnimalRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.Optional;


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
}
