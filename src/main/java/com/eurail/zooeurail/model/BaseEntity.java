package com.eurail.zooeurail.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;
import java.util.UUID;


/**
 * Abstract base entity class for all domain entities in the zoo management system.
 * <p>
 * This class provides common fields and behavior shared across all entities, including
 * unique identification, human-readable titles, and automatic timestamp tracking for
 * creation and update operations.
 * </p>
 * <p>
 * The entity is designed to work with DynamoDB and includes appropriate annotations
 * for JSON serialization and OpenAPI documentation.
 * </p>
 *
 * @see Animal
 * @see Room
 */
@Data
@DynamoDbBean
public abstract class BaseEntity {

    /**
     * Auto-generated unique identifier for the entity.
     * <p>
     * This field is automatically populated with a random UUID upon entity creation
     * and serves as the partition key in DynamoDB. It is read-only and cannot be
     * modified through API requests.
     * </p>
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Auto-generated unique identifier")
    private String id = UUID.randomUUID().toString();

    /**
     * Human-friendly name or title of the entity.
     * <p>
     * This field must not be blank and provides a readable identifier for the entity.
     * It is typically used for display purposes and searching.
     * </p>
     */
    @NotBlank
    @Schema(description = "Human-friendly name/title of the entity", example = "Tiger")
    private String title;

    /**
     * Timestamp when the entity was created.
     * <p>
     * This field is automatically set when the entity is first persisted and is
     * read-only. The timestamp is in ISO-8601 UTC format.
     * </p>
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Creation timestamp (ISO-8601 UTC)")
    private Instant created;

    /**
     * Timestamp when the entity was last updated.
     * <p>
     * This field is automatically updated whenever the entity is modified and is
     * read-only. The timestamp is in ISO-8601 UTC format.
     * </p>
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Last update timestamp (ISO-8601 UTC)")
    private Instant updated;

    /**
     * Gets the unique identifier of the entity.
     * <p>
     * This method is annotated as the DynamoDB partition key and is used by
     * the DynamoDB Enhanced Client for table operations.
     * </p>
     *
     * @return the unique identifier
     */
    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
}