package com.eurail.zooeurail;

import com.eurail.zooeurail.model.Animal;
import com.eurail.zooeurail.model.Room;
import com.eurail.zooeurail.support.DynamoDbTestBase;
import com.eurail.zooeurail.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import com.eurail.zooeurail.repository.AnimalRepository;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "aws.dynamodb.createTablesOnStartup=true"
        })
@ActiveProfiles("test")
class ZooApplicationTests extends DynamoDbTestBase {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    AnimalRepository animalRepository;

    @Autowired
    RoomRepository roomRepository;

    String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
    }

    @Test
    void end_to_end_smoke_flow() {

        // Create a room (persist directly via repository to avoid HTTP flakiness)
        Room room = new Room();
        room.setTitle("Green");
        room = roomRepository.save(room);
        assertThat(room.getId()).isNotBlank();

        // Create an animal (persist directly via repository)
        Animal tiger = new Animal();
        tiger.setTitle("Tiger");
        tiger = animalRepository.save(tiger);
        assertThat(tiger.getId()).isNotBlank();

        // Place animal in the room
        LocalDate today = LocalDate.now();
        ResponseEntity<Animal> placeResp = rest.postForEntity(
                baseUrl + "/animals/" + tiger.getId() + "/place?roomId=" + room.getId() + "&located=" + today,
                null,
                Animal.class);
        assertThat(placeResp.getStatusCode().is2xxSuccessful()).isTrue();

        // Read back and verify location
        Animal fetched = rest.getForObject(baseUrl + "/animals/" + tiger.getId(), Animal.class);
        assertThat(fetched).isNotNull();
        assertThat(fetched.getRoomId()).isEqualTo(room.getId());
    }
}
