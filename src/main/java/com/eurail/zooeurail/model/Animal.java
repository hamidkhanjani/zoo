package com.eurail.zooeurail.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;


@Data
@EqualsAndHashCode(callSuper = true)
@DynamoDbBean
public class Animal extends BaseEntity {

    /** Current room ID where animal is located (nullable). */
    @Schema(description = "Current room id where the animal is located. Nullable if not in any room.")
    private String roomId;

    /** Date when the animal was located (YYYY-MM-DD). System-managed: not accepted in CRUD requests. */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "System-managed date when the animal was located in its current room (YYYY-MM-DD).\n"
            + "Set via place/move operations (defaults to today if omitted), cleared on remove. Returned in responses.")
    private LocalDate located;

    /** Favorite room IDs (can be empty). Note: DynamoDB doesn't allow empty sets; return null when empty. */
    @Schema(description = "Set of room ids marked as the animal's favorites. Empty set is serialized as null due to DynamoDB constraints.")
    private Set<String> favoriteRoomIds = new HashSet<>();

    public Set<String> getFavoriteRoomIds() {
        return (favoriteRoomIds == null || favoriteRoomIds.isEmpty()) ? null : favoriteRoomIds;
    }

    public void setFavoriteRoomIds(Set<String> favoriteRoomIds) {
        if (favoriteRoomIds == null || favoriteRoomIds.isEmpty()) {
            this.favoriteRoomIds = new HashSet<>();
        } else {
            this.favoriteRoomIds = favoriteRoomIds;
        }
    }

}
