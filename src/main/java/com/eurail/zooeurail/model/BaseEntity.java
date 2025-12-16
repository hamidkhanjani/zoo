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


@Data
@DynamoDbBean
public abstract class BaseEntity {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(readOnly = true, description = "Auto-generated unique identifier")
    private String id = UUID.randomUUID().toString();

    @NotBlank
    @Schema(description = "Human-friendly name/title of the entity", example = "Tiger")
    private String title;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(readOnly = true, description = "Creation timestamp (ISO-8601 UTC)")
    private Instant created;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(readOnly = true, description = "Last update timestamp (ISO-8601 UTC)")
    private Instant updated;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
}
