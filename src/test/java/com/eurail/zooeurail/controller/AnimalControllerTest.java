package com.eurail.zooeurail.controller;

import com.eurail.zooeurail.model.Animal;
import com.eurail.zooeurail.service.AnimalService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AnimalController.class)
class AnimalControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    AnimalService animalService;


    @Test
    void create_animal_returns_200_and_null_updated() throws Exception {
        Animal saved = new Animal();
        saved.setId("a-1");
        saved.setTitle("Tiger");
        saved.setCreated(Instant.parse("2024-01-01T00:00:00Z"));
        saved.setUpdated(Instant.parse("2024-01-02T00:00:00Z")); // controller should null this

        Mockito.when(animalService.create(any(Animal.class))).thenReturn(saved);

        mvc.perform(post("/api/animals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tiger\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("a-1"))
                .andExpect(jsonPath("$.title").value("Tiger"))
                .andExpect(jsonPath("$.created").value("2024-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.updated", Matchers.nullValue()));
    }

    @Test
    void get_animal_found_and_not_found() throws Exception {
        Animal found = new Animal();
        found.setId("a-9");
        found.setTitle("Cat");
        Mockito.when(animalService.get("a-9")).thenReturn(Optional.of(found));
        Mockito.when(animalService.get("missing")).thenReturn(Optional.empty());

        mvc.perform(get("/api/animals/{id}", "a-9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("a-9"))
                .andExpect(jsonPath("$.title").value("Cat"));

        mvc.perform(get("/api/animals/{id}", "missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_animal_returns_200() throws Exception {
        Animal updated = new Animal();
        updated.setId("a-7");
        updated.setTitle("Lion");
        updated.setCreated(Instant.parse("2024-01-01T00:00:00Z"));
        updated.setUpdated(Instant.parse("2024-01-03T00:00:00Z"));
        Mockito.when(animalService.update(eq("a-7"), any(Animal.class))).thenReturn(updated);

        mvc.perform(put("/api/animals/{id}", "a-7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Lion\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("a-7"))
                .andExpect(jsonPath("$.title").value("Lion"))
                .andExpect(jsonPath("$.updated").value("2024-01-03T00:00:00Z"));
    }

    @Test
    void delete_animal_returns_204() throws Exception {
        mvc.perform(delete("/api/animals/{id}", "a-5"))
                .andExpect(status().isNoContent());
        Mockito.verify(animalService).delete("a-5");
    }



    @Test
    void place_returns_200_or_404() throws Exception {
        Animal placed = new Animal();
        placed.setId("a-1");
        placed.setTitle("Tiger");
        placed.setRoomId("r-1");
        placed.setLocated(LocalDate.parse("2024-01-10"));

        Mockito.when(animalService.placeInRoom("a-1", "r-1", LocalDate.parse("2024-01-10")))
                .thenReturn(Optional.of(placed));
        Mockito.when(animalService.placeInRoom("missing", "r-1", LocalDate.parse("2024-01-10")))
                .thenReturn(Optional.empty());

        mvc.perform(post("/api/animals/{id}/place", "a-1")
                        .param("roomId", "r-1")
                        .param("located", "2024-01-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value("r-1"))
                .andExpect(jsonPath("$.located").value("2024-01-10"));

        mvc.perform(post("/api/animals/{id}/place", "missing")
                        .param("roomId", "r-1")
                        .param("located", "2024-01-10"))
                .andExpect(status().isNotFound());
    }

    @Test
    void move_returns_200() throws Exception {
        Animal moved = new Animal();
        moved.setId("a-2");
        moved.setTitle("Dog");
        moved.setRoomId("r-2");
        moved.setLocated(LocalDate.parse("2024-01-11"));

        Mockito.when(animalService.moveRoom("a-2", "r-2", LocalDate.parse("2024-01-11")))
                .thenReturn(Optional.of(moved));

        mvc.perform(post("/api/animals/{id}/move", "a-2")
                        .param("roomId", "r-2")
                        .param("located", "2024-01-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value("r-2"))
                .andExpect(jsonPath("$.located").value("2024-01-11"));
    }

    @Test
    void remove_returns_200_or_404() throws Exception {
        Animal afterRemove = new Animal();
        afterRemove.setId("a-3");
        afterRemove.setTitle("Fox");
        afterRemove.setRoomId(null);

        Mockito.when(animalService.removeFromRoom("a-3")).thenReturn(Optional.of(afterRemove));
        Mockito.when(animalService.removeFromRoom("missing")).thenReturn(Optional.empty());

        mvc.perform(delete("/api/animals/{id}/remove", "a-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId", Matchers.nullValue()));

        mvc.perform(delete("/api/animals/{id}/remove", "missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void favorites_assign_and_unassign() throws Exception {
        Animal afterAssign = new Animal(); afterAssign.setId("a-4"); afterAssign.setTitle("Bear");
        Animal afterUnassign = new Animal(); afterUnassign.setId("a-4"); afterUnassign.setTitle("Bear");

        Mockito.when(animalService.assignFavorite("a-4", "r-1")).thenReturn(Optional.of(afterAssign));
        Mockito.when(animalService.unassignFavorite("a-4", "r-1")).thenReturn(Optional.of(afterUnassign));

        mvc.perform(post("/api/animals/{id}/favorites/assign", "a-4").param("roomId", "r-1"))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/animals/{id}/favorites/unassign", "a-4").param("roomId", "r-1"))
                .andExpect(status().isOk());
    }

    @Test
    void animals_in_room_and_favorites_aggregation() throws Exception {
        Animal a1 = new Animal(); a1.setId("a1"); a1.setTitle("A");
        Animal a2 = new Animal(); a2.setId("a2"); a2.setTitle("B");
        Mockito.when(animalService.getAnimalsInRoom("r-9", "title", "asc", 0, 2))
                .thenReturn(List.of(a1, a2));

        mvc.perform(get("/api/animals/in-room/{roomId}", "r-9")
                        .param("sortBy", "title")
                        .param("order", "asc")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("a1"))
                .andExpect(jsonPath("$[1].id").value("a2"));

        Mockito.when(animalService.favoriteRoomsAggregationByRoomTitle()).thenReturn(Map.of("Green", 2L, "Big", 1L));

        mvc.perform(get("/api/animals/favorites/aggregation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Green").value(2))
                .andExpect(jsonPath("$.Big").value(1));
    }
}
