package com.eurail.zooeurail.repository.dynamodb;

import com.eurail.zooeurail.repository.BaseRepository;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;

@RequiredArgsConstructor
public class DynamoDbRepository<T> implements BaseRepository<T> {

    private final DynamoDbTable<T> table;

    public static <T> DynamoDbRepository<T> of(DynamoDbEnhancedClient client, Class<T> clazz, String tableName) {
        DynamoDbTable<T> table = client.table(tableName, TableSchema.fromBean(clazz));
        return new DynamoDbRepository<>(table);
    }

    @Override
    public T save(T entity) {
        table.putItem(entity);
        return entity;
    }

    @Override
    public Optional<T> findById(String id) {
        return Optional.ofNullable(table.getItem(r -> r.key(k -> k.partitionValue(id))));
    }

    @Override
    public void deleteById(String id) {
        table.deleteItem(r -> r.key(k -> k.partitionValue(id)));
    }
}
