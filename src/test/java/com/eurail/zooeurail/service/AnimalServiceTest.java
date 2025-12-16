package com.eurail.zooeurail.service;

import com.eurail.zooeurail.model.Animal;
import com.eurail.zooeurail.repository.AnimalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnimalServiceTest {

    @Mock
    AnimalRepository animalRepository;


    @InjectMocks
    AnimalService animalService;

    Animal tiger;

    @BeforeEach
    void setup() {
        tiger = new Animal();
        tiger.setId("a1");
        tiger.setTitle("Tiger");
        // initialize empty favorites set to avoid null from getter semantics
        tiger.setFavoriteRoomIds(new java.util.HashSet<>());
    }

    @Test
    void placeInRoom_updates_room_and_located_when_animal_exists() {
        when(animalRepository.findById("a1")).thenReturn(Optional.of(tiger));
        when(animalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalDate today = LocalDate.now();
        Optional<Animal> result = animalService.placeInRoom("a1", "r1", today);

        assertThat(result).isPresent();
        Animal saved = result.get();
        assertThat(saved.getRoomId()).isEqualTo("r1");
        assertThat(saved.getLocated()).isEqualTo(today);
        verify(animalRepository).save(saved);
    }

    @Test
    void placeInRoom_returns_empty_when_not_found() {
        when(animalRepository.findById("missing")).thenReturn(Optional.empty());
        assertThat(animalService.placeInRoom("missing", "r1", LocalDate.now())).isEmpty();
        verify(animalRepository, never()).save(any());
    }

    @Test
    void moveRoom_delegates_to_placeInRoom() {
        when(animalRepository.findById("a1")).thenReturn(Optional.of(tiger));
        when(animalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalDate d = LocalDate.of(2024, 1, 2);
        Animal moved = animalService.moveRoom("a1", "r2", d).orElseThrow();
        assertThat(moved.getRoomId()).isEqualTo("r2");
        assertThat(moved.getLocated()).isEqualTo(d);
    }

    @Test
    void removeFromRoom_clears_room() {
        tiger.setRoomId("rX");
        when(animalRepository.findById("a1")).thenReturn(Optional.of(tiger));
        when(animalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Animal updated = animalService.removeFromRoom("a1").orElseThrow();
        assertThat(updated.getRoomId()).isNull();
    }

    @Test
    void assign_and_unassign_favorite_rooms() {
        // initialize with one favorite so getter returns non-null set per model semantics
        tiger.setFavoriteRoomIds(new HashSet<>(List.of("r0")));
        when(animalRepository.findById("a1")).thenReturn(Optional.of(tiger));
        when(animalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Animal afterAssign = animalService.assignFavorite("a1", "r1").orElseThrow();
        assertThat(afterAssign.getFavoriteRoomIds()).contains("r0", "r1");

        Animal afterUnassign = animalService.unassignFavorite("a1", "r1").orElseThrow();
        assertThat(afterUnassign.getFavoriteRoomIds()).containsExactlyInAnyOrder("r0");
    }

    @Test
    void getAnimalsInRoom_sorts_by_title_and_paginates() {
        Animal a = new Animal(); a.setId("a"); a.setTitle("Ant");
        Animal b = new Animal(); b.setId("b"); b.setTitle("Bear");
        Animal c = new Animal(); c.setId("c"); c.setTitle("cat"); // lower case to test case-insensitive
        when(animalRepository.findByRoomId("r1")).thenReturn(List.of(c, b, a));

        List<Animal> page0 = animalService.getAnimalsInRoom("r1", "title", "asc", 0, 2);
        assertThat(page0).extracting(Animal::getTitle).containsExactly("Ant", "Bear");

        List<Animal> page1 = animalService.getAnimalsInRoom("r1", "title", "asc", 1, 2);
        assertThat(page1).extracting(Animal::getTitle).containsExactly("cat");

        List<Animal> desc = animalService.getAnimalsInRoom("r1", "title", "desc", 0, 10);
        assertThat(desc).extracting(Animal::getTitle).containsExactly("cat", "Bear", "Ant");
    }

    @Test
    void getAnimalsInRoom_sorts_by_located_nulls_last() {
        Animal a = new Animal(); a.setId("a"); a.setTitle("A"); a.setLocated(LocalDate.of(2024,1,1));
        Animal b = new Animal(); b.setId("b"); b.setTitle("B"); b.setLocated(LocalDate.of(2024,1,5));
        Animal c = new Animal(); c.setId("c"); c.setTitle("C"); // located null
        when(animalRepository.findByRoomId("r1")).thenReturn(List.of(c, b, a));

        List<Animal> asc = animalService.getAnimalsInRoom("r1", "located", "asc", 0, 10);
        assertThat(asc).extracting(Animal::getId).containsExactly("a", "b", "c");

        List<Animal> desc = animalService.getAnimalsInRoom("r1", "located", "desc", 0, 10);
        // In desc order with reversed comparator, nulls come first
        assertThat(desc).extracting(Animal::getId).containsExactly("c", "b", "a");
    }

    @Test
    void favoriteRoomsAggregation_counts_and_filters_by_universe() {
        Animal a1 = new Animal(); a1.setId("a1"); a1.setTitle("A"); a1.setFavoriteRoomIds(new HashSet<>(List.of("r1", "r2")));
        Animal a2 = new Animal(); a2.setId("a2"); a2.setTitle("B"); a2.setFavoriteRoomIds(new HashSet<>(List.of("r2")));
        Animal a3 = new Animal(); a3.setId("a3"); a3.setTitle("C"); // no favorites
        Animal a4 = new Animal(); a4.setId("a4"); a4.setTitle("D"); a4.setFavoriteRoomIds(new HashSet<>(java.util.Arrays.asList("r3", null)));
        when(animalRepository.findAll()).thenReturn(List.of(a1, a2, a3, a4));

        Map<String, Long> counts = animalService.favoriteRoomsAggregation(null);
        assertThat(counts).containsEntry("r1", 1L).containsEntry("r2", 2L).containsEntry("r3", 1L).doesNotContainKey("null");

        Map<String, Long> filtered = animalService.favoriteRoomsAggregation(List.of("r2", "r3"));
        assertThat(filtered.keySet()).containsExactlyInAnyOrder("r2", "r3");
    }
}
