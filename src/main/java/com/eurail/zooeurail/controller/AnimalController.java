package com.eurail.zooeurail.controller;

import com.eurail.zooeurail.model.Animal;
import com.eurail.zooeurail.service.AnimalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing animals in the zoo.
 * <p>
 * Provides endpoints for CRUD operations (inherited from BaseController) and specialized
 * operations for managing animal locations, room assignments, and favorite rooms.
 * </p>
 *
 * @see BaseController
 * @see AnimalService
 * @see Animal
 */
@RestController
@RequestMapping("/api/animals")
@Tag(name = "Animals", description = "Endpoints for managing animals, locations, and favorites")
@Slf4j
public class AnimalController extends BaseController<Animal> {

    private final AnimalService animalService;

    /**
     * Constructs an AnimalController with the specified AnimalService.
     *
     * @param service the animal service to handle business logic
     */
    public AnimalController(AnimalService service) {
        super(service);
        this.animalService = service;
    }

    /**
     * Places an animal in a specific room.
     * <p>
     * Sets the animal's current room and the date when it was located there.
     * If the located date is not provided, the current date is used.
     * </p>
     *
     * @param id      the ID of the animal to place
     * @param roomId  the ID of the target room
     * @param located optional date when the animal was located in the room (YYYY-MM-DD format);
     *                defaults to today if not provided
     * @return ResponseEntity containing the updated Animal if found, or 404 Not Found if the animal doesn't exist
     */
    @PostMapping("/{id}/place")
    @Operation(summary = "Place animal in a room",
            description = "Set the animal's current room. The 'located' date is optional; if omitted, today's date is used.")
    public ResponseEntity<Animal> place(
            @Parameter(description = "Animal id") @PathVariable String id,
            @Parameter(description = "Target room id") @RequestParam String roomId,
            @Parameter(description = "Optional date the animal was located in the room (YYYY-MM-DD). If not provided, defaults to today.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate located) {
        if (log.isDebugEnabled()) log.debug("Place animal id={} roomId={} located={}", id, roomId, located);
        return animalService.placeInRoom(id, roomId, located)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Moves an animal to a different room.
     * <p>
     * This is a convenience wrapper around the place operation for moving an animal
     * from its current location to a new room. The located date can be specified or
     * defaults to today.
     * </p>
     *
     * @param id      the ID of the animal to move
     * @param roomId  the ID of the new room
     * @param located optional date when the animal was located in the new room (YYYY-MM-DD format);
     *                defaults to today if not provided
     * @return ResponseEntity containing the updated Animal if found, or 404 Not Found if the animal doesn't exist
     */
    @PostMapping("/{id}/move")
    @Operation(summary = "Move animal to another room",
            description = "Convenience wrapper around 'place' for moving to a new room. 'located' is optional and defaults to today if omitted.")
    public ResponseEntity<Animal> move(
            @Parameter(description = "Animal id") @PathVariable String id,
            @Parameter(description = "New room id") @RequestParam String roomId,
            @Parameter(description = "Optional date the animal was located in the new room (YYYY-MM-DD). If not provided, defaults to today.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate located) {
        if (log.isDebugEnabled()) log.debug("Move animal id={} to roomId={} located={}", id, roomId, located);
        return animalService.moveRoom(id, roomId, located)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Removes an animal from its current room.
     * <p>
     * Clears the animal's room assignment (roomId) and the located date,
     * effectively marking the animal as not being in any room.
     * </p>
     *
     * @param id the ID of the animal to remove from its room
     * @return ResponseEntity containing the updated Animal if found, or 404 Not Found if the animal doesn't exist
     */
    @DeleteMapping("/{id}/remove")
    @Operation(summary = "Remove animal from its current room",
            description = "Clears the animal's 'roomId' and the 'located' date.")
    public ResponseEntity<Animal> remove(@Parameter(description = "Animal id") @PathVariable String id) {
        if (log.isDebugEnabled()) log.debug("Remove animal id={} from room", id);
        return animalService.removeFromRoom(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Assigns a room as a favorite for the specified animal.
     * <p>
     * Adds the room ID to the animal's set of favorite rooms. If the room is already
     * a favorite, the operation has no effect.
     * </p>
     *
     * @param id     the ID of the animal
     * @param roomId the ID of the room to add as a favorite
     * @return ResponseEntity containing the updated Animal if found, or 404 Not Found if the animal doesn't exist
     */
    @PostMapping("/{id}/favorites/assign")
    @Operation(summary = "Assign a favorite room",
            description = "Adds the room id to the animal's set of favorite rooms.")
    public ResponseEntity<Animal> assignFavorite(
            @Parameter(description = "Animal id") @PathVariable String id,
            @Parameter(description = "Room id to add as favorite") @RequestParam String roomId) {
        if (log.isDebugEnabled()) log.debug("Assign favorite roomId={} to animal id={}", roomId, id);
        return animalService.assignFavorite(id, roomId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Removes a room from the animal's favorites.
     * <p>
     * Removes the room ID from the animal's set of favorite rooms. If the room is not
     * currently a favorite, the operation has no effect.
     * </p>
     *
     * @param id     the ID of the animal
     * @param roomId the ID of the room to remove from favorites
     * @return ResponseEntity containing the updated Animal if found, or 404 Not Found if the animal doesn't exist
     */
    @DeleteMapping("/{id}/favorites/unassign")
    @Operation(summary = "Unassign a favorite room",
            description = "Removes the room id from the animal's favorites.")
    public ResponseEntity<Animal> unassignFavorite(
            @Parameter(description = "Animal id") @PathVariable String id,
            @Parameter(description = "Room id to remove from favorites") @RequestParam String roomId) {
        if (log.isDebugEnabled()) log.debug("Unassign favorite roomId={} from animal id={}", roomId, id);
        return animalService.unassignFavorite(id, roomId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieves a list of animals currently located in a specific room.
     * <p>
     * Returns a paginated and sorted list of animals in the specified room.
     * Sorting can be applied by title or located date, in ascending or descending order.
     * </p>
     *
     * @param roomId the ID of the room
     * @param sortBy the field to sort by: "title" or "located"; defaults to "title"
     * @param order  the sort order: "asc" or "desc"; defaults to "asc"
     * @param page   the page number (0-based); defaults to 0
     * @param size   the number of items per page; defaults to 10
     * @return ResponseEntity containing a list of animals in the specified room
     */
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

    /**
     * Provides an aggregation of favorite rooms by room title.
     * <p>
     * Returns a map where keys are room titles and values are the count of animals
     * that have marked that room as a favorite. Rooms with zero favorites are excluded
     * from the result.
     * </p>
     *
     * @return ResponseEntity containing a map of room titles to favorite counts
     */
    @GetMapping("/favorites/aggregation")
    @Operation(summary = "Favorite rooms aggregation",
            description = "Returns a map of room titles to the number of animals for which the room is marked as favorite. " +
                    "Rooms with zero favorites are omitted.")
    public ResponseEntity<Map<String, Long>> favoriteRoomsAggregation() {
        // Return mapping of Room Title -> Count, as requested
        return ResponseEntity.ok(animalService.favoriteRoomsAggregationByRoomTitle());
    }
}