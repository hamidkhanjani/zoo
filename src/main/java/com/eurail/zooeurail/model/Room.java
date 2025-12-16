package com.eurail.zooeurail.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;



/**
 * Room entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DynamoDbBean
public class Room extends BaseEntity {
}
