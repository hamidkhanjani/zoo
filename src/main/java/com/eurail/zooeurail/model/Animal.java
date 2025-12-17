package com.eurail.zooeurail.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;


/**
 * Represents an Animal entity in the zoo management system.
 * <p>
 * An animal extends the base entity with location tracking and room preferences.
 * It maintains information about the animal's current room placement, the date
 * when it was located in that room, and a set of favorite rooms.
 * </p>
 * <p>
 * This entity is stored in DynamoDB and includes specific handling for empty sets
 * due to DynamoDB constraints (empty sets are not allowed and are represented as null).
 * </p>
 *
 * @see BaseEntity
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DynamoDbBean
public class Animal extends BaseEntity {

    /**
     * Current room ID where animal is located (nullable).
     */
    @Schema(description = "Current room id where the animal is located. Nullable if not in any room.")
    private String roomId;

    /**
     * Date when the animal was located (YYYY-MM-DD). System-managed: not accepted in CRUD requests.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "System-managed date when the animal was located in its current room (YYYY-MM-DD).\n"
            + "Set via place/move operations (defaults to today if omitted), cleared on remove. Returned in responses.")
    private LocalDate located;

    /**
     * Favorite room IDs (can be empty). Note: DynamoDB doesn't allow empty sets; return null when empty.
     */
    @Schema(description = "Set of room ids marked as the animal's favorites. Empty set is serialized as null due to DynamoDB constraints.")
    private Set<String> favoriteRoomIds = new HashSet<>();

    /**
     * Gets the set of favorite room IDs for this animal.
     * <p>
     * Due to DynamoDB constraints that do not allow empty sets, this method
     * returns {@code null} when the internal set is empty or null.
     * </p>
     *
     * @return the set of favorite room IDs, or {@code null} if empty
     */
    public Set<String> getFavoriteRoomIds() {
        return (favoriteRoomIds == null || favoriteRoomIds.isEmpty()) ? null : favoriteRoomIds;
    }

    /**
     * Sets the favorite room IDs for this animal.
     * <p>
     * If the provided set is {@code null} or empty, the internal set is
     * initialized as an empty {@code HashSet}. Otherwise, the provided set
     * is stored directly.
     * </p>
     *
     * @param favoriteRoomIds the set of favorite room IDs to set
     */
    public void setFavoriteRoomIds(Set<String> favoriteRoomIds) {
        if (favoriteRoomIds == null || favoriteRoomIds.isEmpty()) {
            this.favoriteRoomIds = new HashSet<>();
        } else {
            this.favoriteRoomIds = favoriteRoomIds;
        }
    }

}