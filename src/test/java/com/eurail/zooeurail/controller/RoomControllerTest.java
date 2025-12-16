package com.eurail.zooeurail.controller;

import com.eurail.zooeurail.model.Room;
import com.eurail.zooeurail.service.RoomService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RoomController.class)
class RoomControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    RoomService roomService;

    @Test
    void create_room_returns_200_and_null_updated() throws Exception {
        Room saved = new Room();
        saved.setId("r-1");
        saved.setTitle("Green");
        saved.setCreated(Instant.parse("2024-01-01T00:00:00Z"));
        saved.setUpdated(Instant.parse("2024-01-02T00:00:00Z")); // controller should null this

        Mockito.when(roomService.create(any(Room.class))).thenReturn(saved);

        mvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Green\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("r-1"))
                .andExpect(jsonPath("$.title").value("Green"))
                .andExpect(jsonPath("$.created").value("2024-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.updated", Matchers.nullValue()));
    }

    @Test
    void get_room_found_and_not_found() throws Exception {
        Room found = new Room();
        found.setId("r-42");
        found.setTitle("Blue");

        Mockito.when(roomService.get("r-42")).thenReturn(Optional.of(found));
        Mockito.when(roomService.get("missing")).thenReturn(Optional.empty());

        mvc.perform(get("/api/rooms/{id}", "r-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("r-42"))
                .andExpect(jsonPath("$.title").value("Blue"));

        mvc.perform(get("/api/rooms/{id}", "missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_room_returns_200() throws Exception {
        Room updated = new Room();
        updated.setId("r-9");
        updated.setTitle("Renamed");
        updated.setCreated(Instant.parse("2024-01-01T00:00:00Z"));
        updated.setUpdated(Instant.parse("2024-01-03T00:00:00Z"));

        Mockito.when(roomService.update(eq("r-9"), any(Room.class))).thenReturn(updated);

        mvc.perform(put("/api/rooms/{id}", "r-9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("r-9"))
                .andExpect(jsonPath("$.title").value("Renamed"))
                .andExpect(jsonPath("$.updated").value("2024-01-03T00:00:00Z"));
    }

    @Test
    void delete_room_returns_204() throws Exception {
        mvc.perform(delete("/api/rooms/{id}", "r-7"))
                .andExpect(status().isNoContent());
        Mockito.verify(roomService).delete("r-7");
    }
}
