package com.eurail.zooeurail.service;

import com.eurail.zooeurail.model.Room;
import com.eurail.zooeurail.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    RoomRepository roomRepository;

    @InjectMocks
    RoomService roomService;

    Room room;

    @BeforeEach
    void setup() {
        room = new Room();
        room.setId("r1");
        room.setTitle("Blue");
    }

    @Test
    void create_setsCreated_and_callsSave() {
        // when saving, repository returns the same instance
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Room created = roomService.create(room);

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());

        Room saved = captor.getValue();
        assertThat(saved.getCreated()).as("created timestamp set").isNotNull();
        assertThat(saved.getUpdated()).as("updated should be null on create").isNull();
        assertThat(created).isSameAs(saved);
    }

    @Test
    void update_preservesCreated_setsUpdated_and_setsId() {
        // existing entity with created timestamp
        Room existing = new Room();
        existing.setId("r1");
        existing.setTitle("Old");
        existing.setCreated(Instant.parse("2020-01-01T00:00:00Z"));

        when(roomRepository.findById("r1")).thenReturn(Optional.of(existing));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Room incoming = new Room();
        incoming.setId("ignored");
        incoming.setTitle("New");

        Room result = roomService.update("r1", incoming);

        assertThat(result.getId()).isEqualTo("r1");
        assertThat(result.getCreated()).isEqualTo(existing.getCreated());
        assertThat(result.getUpdated()).isNotNull();
        assertThat(result.getTitle()).isEqualTo("New");
    }

    @Test
    void get_and_delete_delegate_to_repository() {
        when(roomRepository.findById("r1")).thenReturn(Optional.of(room));

        assertThat(roomService.get("r1")).contains(room);
        verify(roomRepository).findById("r1");

        roomService.delete("r1");
        verify(roomRepository).deleteById("r1");
    }
}
