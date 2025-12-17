package com.eurail.zooeurail.service;

import com.eurail.zooeurail.model.Animal;
import com.eurail.zooeurail.model.Room;
import com.eurail.zooeurail.repository.AnimalRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.time.LocalDate;
import java.util.*;

public class AnimalService extends BaseService<Animal> {

    private final RoomService roomService;
    private final AnimalRepository animalRepository;

    public AnimalService(AnimalRepository animalRepository, RoomService roomService) {
        super(animalRepository);
        this.roomService = roomService;
        this.animalRepository = animalRepository;
    }

    @CacheEvict(cacheNames = {"animalsById"}, key = "#animalId")
    public Optional<Animal> placeInRoom(String animalId, String roomId, LocalDate located) {
        return animalRepository.findById(animalId).map(a -> {
            a.setRoomId(roomId);
            a.setLocated(located != null ? located : LocalDate.now());
            return repository.save(a);
        });
    }

    @CacheEvict(cacheNames = {"animalsById"}, key = "#animalId")
    public Optional<Animal> moveRoom(String animalId, String newRoomId, LocalDate located) {
        return placeInRoom(animalId, newRoomId, located);
    }

    @CacheEvict(cacheNames = {"animalsById"}, key = "#animalId")
    public Optional<Animal> removeFromRoom(String animalId) {
        return animalRepository.findById(animalId).map(a -> {
            a.setRoomId(null);
            a.setLocated(null);
            return repository.save(a);
        });
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "animalsById", key = "#animalId"),
            @CacheEvict(cacheNames = "favoriteRoomsAggByTitle", allEntries = true)
    })
    public Optional<Animal> assignFavorite(String animalId, String roomId) {
        return animalRepository.findById(animalId).map(a -> {
            Set<String> favs = new HashSet<>(Optional.ofNullable(a.getFavoriteRoomIds()).orElseGet(Set::of));
            favs.add(roomId);
            a.setFavoriteRoomIds(favs);
            return repository.save(a);
        });
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "animalsById", key = "#animalId"),
            @CacheEvict(cacheNames = "favoriteRoomsAggByTitle", allEntries = true)
    })
    public Optional<Animal> unassignFavorite(String animalId, String roomId) {
        return animalRepository.findById(animalId).map(a -> {
            Set<String> favs = new HashSet<>(Optional.ofNullable(a.getFavoriteRoomIds()).orElseGet(Set::of));
            favs.remove(roomId);
            a.setFavoriteRoomIds(favs);
            return repository.save(a);
        });
    }

    public List<Animal> getAnimalsInRoom(String roomId, String sortBy, String order, int page, int size) {
        if (size <= 0 || page < 0) return java.util.Collections.emptyList();

        SortField sortField = SortField.from(sortBy);
        SortOrder sortOrder = SortOrder.from(order);

        long requiredLong = (long) (page + 1) * (long) size;
        if (requiredLong <= 0L) return java.util.Collections.emptyList();
        int required = (requiredLong > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) requiredLong;

        List<Animal> firstN = animalRepository.findByRoomIdFirstN(roomId, required);

        Comparator<Animal> comparator = comparatorFor(sortField, sortOrder);

        List<Animal> sorted = firstN.stream().sorted(comparator).toList();
        int from = Math.min(sorted.size(), Math.multiplyExact(page, size));
        int to = Math.min(sorted.size(), from + size);
        if (from >= to) return java.util.Collections.emptyList();
        return sorted.subList(from, to);
    }

    public Map<String, Long> favoriteRoomsAggregation(Collection<String> roomIdsUniverse) {
        return animalRepository.aggregateFavoriteRoomCounts(roomIdsUniverse);
    }

    public Map<String, Long> favoriteRoomsAggregation() {
        return animalRepository.aggregateFavoriteRoomCounts();
    }

    /**
     * Aggregation of favorite rooms keyed by room title instead of id.
     * Rooms with no favorites are excluded. If a room id has no matching room (deleted), it is skipped.
     * Uses RoomService to leverage roomsById cache and reduce DynamoDB calls.
     */
    @Cacheable(cacheNames = "favoriteRoomsAggByTitle")
    public Map<String, Long> favoriteRoomsAggregationByRoomTitle() {
        Map<String, Long> byId = favoriteRoomsAggregation();
        if (byId.isEmpty()) return java.util.Collections.emptyMap();

        Map<String, Long> byTitle = new HashMap<>();

        byId.entrySet().stream()
                .filter(e -> e.getKey() != null)
                .filter(e -> e.getValue() != null && e.getValue() > 0)
                .forEach(e -> roomService.get(e.getKey())
                        .map(Room::getTitle)
                        .filter(title -> !title.isBlank())
                        .ifPresent(title -> byTitle.merge(title, e.getValue(), Long::sum)));

        return byTitle.isEmpty() ? java.util.Collections.emptyMap() : java.util.Map.copyOf(byTitle);
    }

    private static Comparator<Animal> comparatorFor(SortField sortField, SortOrder sortOrder) {
        Comparator<Animal> base = switch (sortField) {
            case LOCATED -> Comparator.comparing(Animal::getLocated, Comparator.nullsLast(Comparator.naturalOrder()));
            case TITLE -> Comparator.comparing(Animal::getTitle, Comparator.nullsLast(String::compareToIgnoreCase));
        };

        // Important for stable pagination (avoid “random” ordering for equal fields)
        base = base.thenComparing(Animal::getId, Comparator.nullsLast(String::compareToIgnoreCase));

        return (sortOrder == SortOrder.DESC) ? base.reversed() : base;
    }

    private enum SortField {
        TITLE, LOCATED;

        static SortField from(String raw) {
            if ("located".equalsIgnoreCase(raw)) return LOCATED;
            return TITLE;
        }
    }

    private enum SortOrder {
        ASC, DESC;

        static SortOrder from(String raw) {
            if ("desc".equalsIgnoreCase(raw)) return DESC;
            return ASC;
        }
    }

    // Cache-enabled overrides for CRUD paths
    @Override
    @Cacheable(cacheNames = "animalsById", key = "#id")
    public Optional<Animal> get(String id) {
        return super.get(id);
    }

    @Override
    @CacheEvict(cacheNames = {"animalsById"}, key = "#result.id", condition = "#result != null")
    public Animal create(Animal entity) {
        return super.create(entity);
    }

    @Override
    @CacheEvict(cacheNames = {"animalsById"}, key = "#id")
    public Animal update(String id, Animal updated) {
        return super.update(id, updated);
    }

    @Override
    @CacheEvict(cacheNames = {"animalsById"}, key = "#id")
    public void delete(String id) {
        super.delete(id);
    }
}
