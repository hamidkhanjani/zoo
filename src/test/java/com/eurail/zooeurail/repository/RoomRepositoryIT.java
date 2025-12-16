package com.eurail.zooeurail.repository;

import com.eurail.zooeurail.model.Room;
import com.eurail.zooeurail.support.DynamoDbTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "aws.dynamodb.createTablesOnStartup=true"
})
class RoomRepositoryIT extends DynamoDbTestBase {

    @Autowired
    RoomRepository roomRepository;

    @Test
    void save_and_find_room() {
        Room r1 = new Room();
        r1.setTitle("Green");
        Room r2 = new Room();
        r2.setTitle("Blue");

        r1 = roomRepository.save(r1);
        r2 = roomRepository.save(r2);

        assertThat(r1.getId()).isNotBlank();
        assertThat(r2.getId()).isNotBlank();

        Optional<Room> found1 = roomRepository.findById(r1.getId());
        Optional<Room> found2 = roomRepository.findById(r2.getId());

        assertThat(found1).isPresent();
        assertThat(found1.get().getTitle()).isEqualTo("Green");
        assertThat(found2).isPresent();
        assertThat(found2.get().getTitle()).isEqualTo("Blue");
    }

    @Test
    void update_room() {
        Room r = new Room();
        r.setTitle("Temp");
        r = roomRepository.save(r);

        r.setTitle("Updated");
        roomRepository.save(r);

        Optional<Room> found = roomRepository.findById(r.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Updated");
    }

    @Test
    void delete_room() {
        Room r1 = new Room();
        r1.setTitle("A");
        Room r2 = new Room();
        r2.setTitle("B");

        r1 = roomRepository.save(r1);
        r2 = roomRepository.save(r2);

        roomRepository.deleteById(r1.getId());

        assertThat(roomRepository.findById(r1.getId())).isEmpty();
        assertThat(roomRepository.findById(r2.getId())).isPresent();
    }
}
