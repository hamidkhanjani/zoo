package com.eurail.zooeurail.service;

import com.eurail.zooeurail.model.Animal;
import com.eurail.zooeurail.model.Room;
import com.eurail.zooeurail.repository.AnimalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.time.LocalDate;
import java.util.*;

/**
 * Service class for managing {@link Animal} entities in the zoo management system.
 * <p>
 * This service extends {@link BaseService} and provides comprehensive functionality for
 * managing animals including room placement, favorite room assignments, and aggregation
 * operations. It includes caching support for improved performance using Spring's cache
 * abstraction.
 * </p>
 * <p>
 * The service manages two cache regions:
 * <ul>
 *   <li>{@code animalsById}: caches individual animals by their unique identifier</li>
 *   <li>{@code favoriteRoomsAggByTitle}: caches aggregated favorite room counts by room title</li>
 * </ul>
 * </p>
 *
 * @see Animal
 * @see BaseService
 * @see RoomService
 */
@Slf4j
public class AnimalService extends BaseService<Animal> {

    private final RoomService roomService;
    private final AnimalRepository animalRepository;

    /**
     * Constructs a new AnimalService with the required dependencies.
     *
     * @param animalRepository the repository for animal data access operations
     * @param roomService      the service for room-related operations
     */
    public AnimalService(AnimalRepository animalRepository, RoomService roomService) {
        super(animalRepository);
        this.roomService = roomService;
        this.animalRepository = animalRepository;
    }

    /**
     * Places an animal in a specific room with an optional location date.
     * <p>
     * If the animal is already in a room, this operation will update its room assignment.
     * If no location date is provided, the current date is used.
     * </p>
     * <p>
     * This operation evicts the animal from the cache to ensure consistency.
     * </p>
     *
     * @param animalId the unique identifier of the animal to place
     * @param roomId   the unique identifier of the room where the animal will be placed
     * @param located  the date the animal is located in the room, or {@code null} to use the current date
     * @return an {@link Optional} containing the updated animal if found, or {@link Optional#empty()} if not found
     */
    @CacheEvict(cacheNames = {"animalsById"}, key = "#animalId")
    public Optional<Animal> placeInRoom(String animalId, String roomId, LocalDate located) {
        log.info("Place animal id={} into room id={} located={}", animalId, roomId, located);
        return animalRepository.findById(animalId).map(a -> {
            a.setRoomId(roomId);
            a.setLocated(located != null ? located : LocalDate.now());
            Animal saved = repository.save(a);
            if (log.isDebugEnabled()) log.debug("Animal placed: {}", saved);
            return saved;
        });
    }

    /**
     * Moves an animal from its current room to a new room with an optional location date.
     * <p>
     * This is a convenience method that delegates to {@link #placeInRoom(String, String, LocalDate)}.
     * </p>
     *
     * @param animalId  the unique identifier of the animal to move
     * @param newRoomId the unique identifier of the new room
     * @param located   the date the animal is located in the new room, or {@code null} to use the current date
     * @return an {@link Optional} containing the updated animal if found, or {@link Optional#empty()} if not found
     */
    @CacheEvict(cacheNames = {"animalsById"}, key = "#animalId")
    public Optional<Animal> moveRoom(String animalId, String newRoomId, LocalDate located) {
        return placeInRoom(animalId, newRoomId, located);
    }

    /**
     * Removes an animal from its current room.
     * <p>
     * This operation clears the animal's room assignment and location date.
     * The animal remains in the system but is not associated with any room.
     * </p>
     *
     * @param animalId the unique identifier of the animal to remove from its room
     * @return an {@link Optional} containing the updated animal if found, or {@link Optional#empty()} if not found
     */
    @CacheEvict(cacheNames = {"animalsById"}, key = "#animalId")
    public Optional<Animal> removeFromRoom(String animalId) {
        log.info("Remove animal id={} from its room", animalId);
        return animalRepository.findById(animalId).map(a -> {
            a.setRoomId(null);
            a.setLocated(null);
            Animal saved = repository.save(a);
            if (log.isDebugEnabled()) log.debug("Animal removed from room: {}", saved);
            return saved;
        });
    }

    /**
     * Assigns a room as a favorite for a specific animal.
     * <p>
     * An animal can have multiple favorite rooms. If the room is already a favorite,
     * this operation has no effect.
     * </p>
     * <p>
     * This operation evicts both the specific animal cache and the favorite rooms
     * aggregation cache to maintain consistency.
     * </p>
     *
     * @param animalId the unique identifier of the animal
     * @param roomId   the unique identifier of the room to mark as favorite
     * @return an {@link Optional} containing the updated animal if found, or {@link Optional#empty()} if not found
     */
    @Caching(evict = {
            @CacheEvict(cacheNames = "animalsById", key = "#animalId"),
            @CacheEvict(cacheNames = "favoriteRoomsAggByTitle", allEntries = true)
    })
    public Optional<Animal> assignFavorite(String animalId, String roomId) {
        log.info("Assign favorite room id={} to animal id={}", roomId, animalId);
        return animalRepository.findById(animalId).map(a -> {
            Set<String> favs = new HashSet<>(Optional.ofNullable(a.getFavoriteRoomIds()).orElseGet(Set::of));
            favs.add(roomId);
            a.setFavoriteRoomIds(favs);
            Animal saved = repository.save(a);
            if (log.isDebugEnabled()) log.debug("Favorite assigned, animal: {}", saved);
            return saved;
        });
    }

    /**
     * Removes a room from an animal's favorite rooms.
     * <p>
     * If the room is not in the animal's favorites, this operation has no effect.
     * </p>
     * <p>
     * This operation evicts both the specific animal cache and the favorite rooms
     * aggregation cache to maintain consistency.
     * </p>
     *
     * @param animalId the unique identifier of the animal
     * @param roomId   the unique identifier of the room to remove from favorites
     * @return an {@link Optional} containing the updated animal if found, or {@link Optional#empty()} if not found
     */
    @Caching(evict = {
            @CacheEvict(cacheNames = "animalsById", key = "#animalId"),
            @CacheEvict(cacheNames = "favoriteRoomsAggByTitle", allEntries = true)
    })
    public Optional<Animal> unassignFavorite(String animalId, String roomId) {
        log.info("Unassign favorite room id={} from animal id={}", roomId, animalId);
        return animalRepository.findById(animalId).map(a -> {
            Set<String> favs = new HashSet<>(Optional.ofNullable(a.getFavoriteRoomIds()).orElseGet(Set::of));
            favs.remove(roomId);
            a.setFavoriteRoomIds(favs);
            Animal saved = repository.save(a);
            if (log.isDebugEnabled()) log.debug("Favorite unassigned, animal: {}", saved);
            return saved;
        });
    }

    /**
     * Retrieves a paginated and sorted list of animals in a specific room.
     * <p>
     * This method fetches animals from the specified room, sorts them according to the
     * provided criteria, and returns a specific page of results. The sorting is stable,
     * meaning that animals with equal sort field values will maintain consistent ordering
     * across requests based on their IDs.
     * </p>
     * <p>
     * Supported sort fields:
     * <ul>
     *   <li>{@code "located"}: sorts by the date the animal was located in the room</li>
     *   <li>{@code "title"}: sorts by the animal's title (case-insensitive)</li>
     * </ul>
     * Sort order can be {@code "asc"} (ascending) or {@code "desc"} (descending).
     * </p>
     *
     * @param roomId the unique identifier of the room
     * @param sortBy the field to sort by ({@code "title"} or {@code "located"})
     * @param order  the sort order ({@code "asc"} or {@code "desc"})
     * @param page   the zero-based page number to retrieve
     * @param size   the number of animals per page
     * @return a list of animals for the specified page, or an empty list if the page is out of bounds
     * or parameters are invalid
     */
    public List<Animal> getAnimalsInRoom(String roomId, String sortBy, String order, int page, int size) {
        if (size <= 0 || page < 0) return Collections.emptyList();

        SortField sortField = SortField.from(sortBy);
        SortOrder sortOrder = SortOrder.from(order);

        long requiredLong = (long) (page + 1) * (long) size;
        if (requiredLong <= 0L) return Collections.emptyList();
        int required = (requiredLong > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) requiredLong;

        List<Animal> firstN = animalRepository.findByRoomIdFirstN(roomId, required);

        Comparator<Animal> comparator = comparatorFor(sortField, sortOrder);

        List<Animal> sorted = firstN.stream().sorted(comparator).toList();
        int from = Math.min(sorted.size(), Math.multiplyExact(page, size));
        int to = Math.min(sorted.size(), from + size);
        if (from >= to) return Collections.emptyList();
        List<Animal> pageList = sorted.subList(from, to);
        if (log.isDebugEnabled()) log.debug("Returning {} animals for page {}", pageList.size(), page);
        return pageList;
    }

    /**
     * Aggregates the count of animals that have marked each room as a favorite,
     * filtered by a specific set of room IDs.
     * <p>
     * Only rooms whose IDs are present in the {@code roomIdsUniverse} collection
     * will be included in the aggregation results.
     * </p>
     *
     * @param roomIdsUniverse a collection of room IDs to include in the aggregation
     * @return a map where keys are room IDs and values are the count of animals that favorited each room
     */
    public Map<String, Long> favoriteRoomsAggregation(Collection<String> roomIdsUniverse) {
        return animalRepository.aggregateFavoriteRoomCounts(roomIdsUniverse);
    }

    /**
     * Aggregates the count of animals that have marked each room as a favorite.
     * <p>
     * This method includes all rooms that have been favorited by at least one animal.
     * </p>
     *
     * @return a map where keys are room IDs and values are the count of animals that favorited each room
     */
    public Map<String, Long> favoriteRoomsAggregation() {
        return animalRepository.aggregateFavoriteRoomCounts();
    }

    /**
     * Aggregation of favorite rooms keyed by room title instead of id.
     * Rooms with no favorites are excluded. If a room id has no matching room (deleted), it is skipped.
     * Uses RoomService to leverage roomsById cache and reduce DynamoDB calls.
     *
     * @return a map where keys are room titles and values are the count of animals that favorited rooms with that title
     */
    @Cacheable(cacheNames = "favoriteRoomsAggByTitle")
    public Map<String, Long> favoriteRoomsAggregationByRoomTitle() {
        Map<String, Long> byId = favoriteRoomsAggregation();
        if (byId.isEmpty()) return Collections.emptyMap();

        Map<String, Long> byTitle = new HashMap<>();

        byId.entrySet().stream()
                .filter(e -> e.getKey() != null)
                .filter(e -> e.getValue() != null && e.getValue() > 0)
                .forEach(e -> roomService.get(e.getKey())
                        .map(Room::getTitle)
                        .filter(title -> !title.isBlank())
                        .ifPresent(title -> byTitle.merge(title, e.getValue(), Long::sum)));

        return byTitle.isEmpty() ? java.util.Collections.emptyMap() : Map.copyOf(byTitle);
    }

    /**
     * Creates a comparator for sorting animals based on the specified field and order.
     * <p>
     * The comparator ensures stable pagination by using the animal ID as a secondary
     * sort key when the primary sort field values are equal.
     * </p>
     *
     * @param sortField the field to sort by
     * @param sortOrder the sort order (ascending or descending)
     * @return a comparator for sorting animals
     */
    private static Comparator<Animal> comparatorFor(SortField sortField, SortOrder sortOrder) {
        Comparator<Animal> base = switch (sortField) {
            case LOCATED -> Comparator.comparing(Animal::getLocated, Comparator.nullsLast(Comparator.naturalOrder()));
            case TITLE -> Comparator.comparing(Animal::getTitle, Comparator.nullsLast(String::compareToIgnoreCase));
        };

        // Important for stable pagination (avoid "random" ordering for equal fields)
        base = base.thenComparing(Animal::getId, Comparator.nullsLast(String::compareToIgnoreCase));

        return (sortOrder == SortOrder.DESC) ? base.reversed() : base;
    }

    /**
     * Enum representing supported sort fields for animal queries.
     */
    private enum SortField {
        /**
         * Sort by animal title
         */
        TITLE,
        /**
         * Sort by location date
         */
        LOCATED;

        /**
         * Parses a string into a SortField.
         * <p>
         * If the input string equals "located" (case-insensitive), returns {@code LOCATED}.
         * Otherwise, returns {@code TITLE} as the default.
         * </p>
         *
         * @param raw the string to parse
         * @return the corresponding SortField
         */
        static SortField from(String raw) {
            if ("located".equalsIgnoreCase(raw)) return LOCATED;
            return TITLE;
        }
    }

    /**
     * Enum representing sort order options.
     */
    private enum SortOrder {
        /**
         * Ascending order
         */
        ASC,
        /**
         * Descending order
         */
        DESC;

        /**
         * Parses a string into a SortOrder.
         * <p>
         * If the input string equals "desc" (case-insensitive), returns {@code DESC}.
         * Otherwise, returns {@code ASC} as the default.
         * </p>
         *
         * @param raw the string to parse
         * @return the corresponding SortOrder
         */
        static SortOrder from(String raw) {
            if ("desc".equalsIgnoreCase(raw)) return DESC;
            return ASC;
        }
    }

    // Cache-enabled overrides for CRUD paths

    /**
     * Retrieves an animal by its unique identifier with caching support.
     * <p>
     * This method overrides the base implementation to add caching. Results are
     * cached in the {@code animalsById} cache region.
     * </p>
     *
     * @param id the unique identifier of the animal
     * @return an {@link Optional} containing the animal if found, or {@link Optional#empty()} if not found
     */
    @Override
    @Cacheable(cacheNames = "animalsById", key = "#id")
    public Optional<Animal> get(String id) {
        return super.get(id);
    }

    /**
     * Creates a new animal and evicts relevant cache entries.
     * <p>
     * After creation, the new animal is cached in the {@code animalsById} cache region.
     * </p>
     *
     * @param entity the animal to create
     * @return the created animal with generated ID and timestamps
     */
    @Override
    @CacheEvict(cacheNames = {"animalsById"}, key = "#result.id", condition = "#result != null")
    public Animal create(Animal entity) {
        return super.create(entity);
    }

    /**
     * Updates an existing animal and evicts it from the cache.
     * <p>
     * This ensures that subsequent reads will fetch the updated animal from the database.
     * </p>
     *
     * @param id      the unique identifier of the animal to update
     * @param updated the animal with updated field values
     * @return the updated animal
     */
    @Override
    @CacheEvict(cacheNames = {"animalsById"}, key = "#id")
    public Animal update(String id, Animal updated) {
        return super.update(id, updated);
    }

    /**
     * Deletes an animal by its unique identifier and evicts it from the cache.
     *
     * @param id the unique identifier of the animal to delete
     */
    @Override
    @CacheEvict(cacheNames = {"animalsById"}, key = "#id")
    public void delete(String id) {
        super.delete(id);
    }
}