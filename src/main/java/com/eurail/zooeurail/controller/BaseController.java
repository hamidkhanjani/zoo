package com.eurail.zooeurail.controller;

import com.eurail.zooeurail.model.BaseEntity;
import com.eurail.zooeurail.service.BaseService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;


public abstract class BaseController<T extends BaseEntity> {

    protected final BaseService<T> service;

    protected BaseController(BaseService<T> service) {
        this.service = service;
    }

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
        T saved = service.create(body);
        // Do not expose audit fields on create response
        saved.setUpdated(null);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get entity by id",
            description = "Fetch a single entity by its identifier.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Found",
                            content = @Content(schema = @Schema(implementation = BaseEntity.class))),
                    @ApiResponse(responseCode = "404", description = "Not Found")
            })
    public ResponseEntity<T> get(@Parameter(description = "Entity id") @PathVariable String id) {
        Optional<T> found = service.get(id);
        return found.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update entity",
            description = "Replace an entity's content. The path 'id' is authoritative and will overwrite any id in the body."
    )
    public ResponseEntity<T> update(
            @Parameter(description = "Entity id") @PathVariable String id,
            @Parameter(description = "Entity payload to save") @Valid @RequestBody T body) {
        return ResponseEntity.ok(service.update(id, body));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete entity",
            description = "Delete a single entity by its id.")
    public ResponseEntity<Void> delete(@Parameter(description = "Entity id") @PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
