package com.eurail.zooeurail.controller;

import com.eurail.zooeurail.model.BaseEntity;
import com.eurail.zooeurail.service.BaseService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Abstract base controller providing CRUD operations for entities extending {@link BaseEntity}.
 * <p>
 * This controller defines common REST endpoints for creating, reading, updating, and deleting entities.
 * Subclasses should specify the concrete entity type and inject the appropriate service implementation.
 * </p>
 *
 * @param <T> the entity type that extends {@link BaseEntity}
 */
@Slf4j
public abstract class BaseController<T extends BaseEntity> {

    /**
     * The service layer responsible for business logic and persistence operations.
     */
    protected final BaseService<T> service;

    /**
     * Constructs a new BaseController with the specified service.
     *
     * @param service the service instance to handle entity operations
     */
    protected BaseController(BaseService<T> service) {
        this.service = service;
    }

    /**
     * Creates a new entity.
     * <p>
     * The entity's ID is auto-generated, the created timestamp is set automatically,
     * and the updated timestamp is null on creation. The updated field is not exposed
     * in the response.
     * </p>
     *
     * @param body the entity payload to create
     * @return a {@link ResponseEntity} containing the created entity with HTTP status 200 (OK)
     */
    @PostMapping
    @Operation(summary = "Create entity",
            description = "Create a new entity. Only the 'title' needs to be provided. " +
                    "'id' is auto-generated, 'created' is set automatically, and 'updated' is null on creation.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Created",
                            content = @Content(schema = @Schema(implementation = BaseEntity.class))),
            })
    public ResponseEntity<T> create(
            @Parameter(description = "Entity payload to create")
            @Valid @RequestBody T body) {
        if (log.isDebugEnabled()) log.debug("Request to create: {}", body);
        T saved = service.create(body);
        // Do not expose audit fields on create response
        saved.setUpdated(null);
        if (log.isInfoEnabled()) log.info("Created id={}", saved.getId());
        return ResponseEntity.ok(saved);
    }

    /**
     * Retrieves an entity by its ID.
     * <p>
     * If the entity is found, it is returned with HTTP status 200 (OK).
     * If the entity is not found, HTTP status 404 (Not Found) is returned.
     * </p>
     *
     * @param id the unique identifier of the entity
     * @return a {@link ResponseEntity} containing the entity if found, or 404 if not found
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get entity by id",
            description = "Fetch a single entity by its identifier.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Found",
                            content = @Content(schema = @Schema(implementation = BaseEntity.class))),
                    @ApiResponse(responseCode = "404", description = "Not Found")
            })
    public ResponseEntity<T> get(@Parameter(description = "Entity id") @PathVariable String id) {
        if (log.isDebugEnabled()) log.debug("Request to get id={}", id);
        Optional<T> found = service.get(id);
        if (found.isEmpty()) {
            log.warn("Entity not found id={}", id);
        }
        return found.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Updates an existing entity.
     * <p>
     * The path ID is authoritative and will overwrite any ID in the request body.
     * The updated timestamp is set automatically by the service layer.
     * </p>
     *
     * @param id   the unique identifier of the entity to update
     * @param body the entity payload containing updated values
     * @return a {@link ResponseEntity} containing the updated entity with HTTP status 200 (OK)
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update entity",
            description = "Replace an entity's content. The path 'id' is authoritative and will overwrite any id in the body."
    )
    public ResponseEntity<T> update(
            @Parameter(description = "Entity id") @PathVariable String id,
            @Parameter(description = "Entity payload to save") @Valid @RequestBody T body) {
        if (log.isDebugEnabled()) log.debug("Request to update id={} with {}", id, body);
        T updated = service.update(id, body);
        if (log.isInfoEnabled()) log.info("Updated id={}", id);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes an entity by its ID.
     * <p>
     * Upon successful deletion, HTTP status 204 (No Content) is returned.
     * </p>
     *
     * @param id the unique identifier of the entity to delete
     * @return a {@link ResponseEntity} with HTTP status 204 (No Content)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete entity",
            description = "Delete a single entity by its id.")
    public ResponseEntity<Void> delete(@Parameter(description = "Entity id") @PathVariable String id) {
        if (log.isDebugEnabled()) log.debug("Request to delete id={}", id);
        service.delete(id);
        if (log.isInfoEnabled()) log.info("Deleted id={}", id);
        return ResponseEntity.noContent().build();
    }
}