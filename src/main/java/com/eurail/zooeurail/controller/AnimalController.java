package com.eurail.zooeurail.controller;

import com.eurail.zooeurail.model.Animal;
import com.eurail.zooeurail.service.AnimalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/animals")
@Tag(name = "Animals", description = "Endpoints for managing animals, locations, and favorites")
public class AnimalController extends BaseController<Animal> {

    private final AnimalService animalService;

    public AnimalController(AnimalService service) {
        super(service);
        this.animalService = service;
    }

    @PostMapping("/{id}/place")
    @Operation(summary = "Place animal in a room",
            description = "Set the animal's current room. The 'located' date is optional; if omitted, today's date is used.")
    public ResponseEntity<Animal> place(
            @Parameter(description = "Animal id") @PathVariable String id,
            @Parameter(description = "Target room id") @RequestParam String roomId,
            @Parameter(description = "Optional date the animal was located in the room (YYYY-MM-DD). If not provided, defaults to today.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate located) {
        return animalService.placeInRoom(id, roomId, located)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/move")
    @Operation(summary = "Move animal to another room",
            description = "Convenience wrapper around 'place' for moving to a new room. 'located' is optional and defaults to today if omitted.")
    public ResponseEntity<Animal> move(
            @Parameter(description = "Animal id") @PathVariable String id,
            @Parameter(description = "New room id") @RequestParam String roomId,
            @Parameter(description = "Optional date the animal was located in the new room (YYYY-MM-DD). If not provided, defaults to today.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate located) {
        return animalService.moveRoom(id, roomId, located)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/remove")
    @Operation(summary = "Remove animal from its current room",
            description = "Clears the animal's 'roomId' and the 'located' date.")
    public ResponseEntity<Animal> remove(@Parameter(description = "Animal id") @PathVariable String id) {
        return animalService.removeFromRoom(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/favorites/assign")
    @Operation(summary = "Assign a favorite room",
            description = "Adds the room id to the animal's set of favorite rooms.")
    public ResponseEntity<Animal> assignFavorite(
            @Parameter(description = "Animal id") @PathVariable String id,
            @Parameter(description = "Room id to add as favorite") @RequestParam String roomId) {
        return animalService.assignFavorite(id, roomId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/favorites/unassign")
    @Operation(summary = "Unassign a favorite room",
            description = "Removes the room id from the animal's favorites.")
    public ResponseEntity<Animal> unassignFavorite(
            @Parameter(description = "Animal id") @PathVariable String id,
            @Parameter(description = "Room id to remove from favorites") @RequestParam String roomId) {
        return animalService.unassignFavorite(id, roomId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/in-room/{roomId}")
    @Operation(summary = "List animals in a specific room",
            description = "Returns animals currently located in the given room with optional sorting and pagination."
    )
    public ResponseEntity<List<Animal>> animalsInRoom(
            @Parameter(description = "Room id") @PathVariable String roomId,
            @Parameter(description = "Sort field: 'title' or 'located'", example = "title")
            @RequestParam(defaultValue = "title") String sortBy,
            @Parameter(description = "Sort order: 'asc' or 'desc'", example = "asc")
            @RequestParam(defaultValue = "asc") String order,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") @Min(1) int size) {
        List<Animal> list = animalService.getAnimalsInRoom(roomId, sortBy, order, page, size);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/favorites/aggregation")
    @Operation(summary = "Favorite rooms aggregation",
            description = "Returns a map of room titles to the number of animals for which the room is marked as favorite. " +
                    "Rooms with zero favorites are omitted.")
    public ResponseEntity<Map<String, Long>> favoriteRoomsAggregation() {
        // Return mapping of Room Title -> Count, as requested
        return ResponseEntity.ok(animalService.favoriteRoomsAggregationByRoomTitle());
    }
}
