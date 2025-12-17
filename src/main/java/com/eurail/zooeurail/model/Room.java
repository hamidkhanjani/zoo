package com.eurail.zooeurail.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;


/**
 * Represents a Room entity in the zoo management system.
 * <p>
 * A room extends the base entity and serves as a location where animals can be placed.
 * Rooms can be associated with animals through their {@code roomId} field and can be
 * marked as favorites by animals through their {@code favoriteRoomIds} collection.
 * </p>
 * <p>
 * This entity is stored in DynamoDB and inherits common fields such as unique identifier,
 * title, and timestamps from {@link BaseEntity}.
 * </p>
 *
 * @see BaseEntity
 * @see Animal
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DynamoDbBean
public class Room extends BaseEntity {
}