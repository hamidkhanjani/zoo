package com.eurail.zooeurail.service;

import com.eurail.zooeurail.model.Animal;
import com.eurail.zooeurail.model.Room;
import com.eurail.zooeurail.repository.AnimalRepository;
import com.eurail.zooeurail.repository.RoomRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class AnimalService extends BaseService<Animal> {

    private final RoomRepository roomRepository;
    private final AnimalRepository animalRepository;

    public AnimalService(AnimalRepository animalRepository, RoomRepository roomRepository) {
        super(animalRepository);
        this.roomRepository = roomRepository;
        this.animalRepository = animalRepository;
    }

    @CacheEvict(cacheNames = {"animalsById"}, key = "#animalId")
    public Optional<Animal> placeInRoom(String animalId, String roomId, LocalDate located) {
        return get(animalId).map(a -> {
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
        return get(animalId).map(a -> {
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
        return get(animalId).map(a -> {
            a.getFavoriteRoomIds().add(roomId);
            return repository.save(a);
        });
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "animalsById", key = "#animalId"),
            @CacheEvict(cacheNames = "favoriteRoomsAggByTitle", allEntries = true)
    })
    public Optional<Animal> unassignFavorite(String animalId, String roomId) {
        return get(animalId).map(a -> {
            a.getFavoriteRoomIds().remove(roomId);
            return repository.save(a);
        });
    }

    public List<Animal> getAnimalsInRoom(String roomId, String sortBy, String order, int page, int size) {
        // Fetch animals using GSI by roomId (no full scan)
        List<Animal> all = animalRepository.findByRoomId(roomId);

        Comparator<Animal> comparator;
        if ("located".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(Animal::getLocated, Comparator.nullsLast(Comparator.naturalOrder()));
        } else { // default title
            comparator = Comparator.comparing(Animal::getTitle, Comparator.nullsLast(String::compareToIgnoreCase));
        }
        if ("desc".equalsIgnoreCase(order)) {
            comparator = comparator.reversed();
        }

        return all.stream()
                .sorted(comparator)
                .skip((long) page * size)
                .limit(size)
                .toList();
    }

    public Map<String, Long> favoriteRoomsAggregation(Collection<String> roomIdsUniverse) {
        List<Animal> all = animalRepository.findAll();
        Map<String, Long> counts = all.stream()
                .flatMap(a -> a.getFavoriteRoomIds() == null ? java.util.stream.Stream.<String>empty() : a.getFavoriteRoomIds().stream())
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        // If a universe list is provided, include only those present in universe; otherwise return non-zero counts only
        if (roomIdsUniverse != null) {
            counts.keySet().retainAll(new java.util.HashSet<>(roomIdsUniverse));
        }
        // Remove zero counts (should not exist) and return map
        return counts.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Aggregation of favorite rooms keyed by room title instead of id.
     * Rooms with no favorites are excluded. If a room id has no matching room (deleted), it is skipped.
     */
    @Cacheable(cacheNames = "favoriteRoomsAggByTitle")
    public Map<String, Long> favoriteRoomsAggregationByRoomTitle() {
        Map<String, Long> byId = favoriteRoomsAggregation(null);
        if (byId.isEmpty()) return java.util.Collections.emptyMap();

        Map<String, Long> byTitle = new java.util.HashMap<>();
        for (Map.Entry<String, Long> e : byId.entrySet()) {
            String roomId = e.getKey();
            Long count = e.getValue();
            roomRepository.findById(roomId)
                    .map(Room::getTitle)
                    .ifPresent(title -> byTitle.merge(title, count, Long::sum));
        }
        return byTitle;
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
