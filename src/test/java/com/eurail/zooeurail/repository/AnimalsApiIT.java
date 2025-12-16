package com.eurail.zooeurail.repository;

import com.eurail.zooeurail.model.Animal;
import com.eurail.zooeurail.model.Room;
import com.eurail.zooeurail.support.DynamoDbTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "aws.dynamodb.createTablesOnStartup=true"
        })
@EnabledIfSystemProperty(named = "withDocker", matches = "true")
public class AnimalsApiIT extends DynamoDbTestBase {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
    }

    @Test
    void fullFlow_animals_crud_and_queries() {
        // Create two rooms
        Room green = new Room();
        green.setTitle("Green");
        green = rest.postForObject(baseUrl + "/rooms", green, Room.class);
        assertThat(green.getId()).isNotBlank();

        Room big = new Room();
        big.setTitle("Big");
        big = rest.postForObject(baseUrl + "/rooms", big, Room.class);
        assertThat(big.getId()).isNotBlank();

        // Create animals
        Animal a1 = new Animal(); a1.setTitle("Tiger");
        Animal a2 = new Animal(); a2.setTitle("Cat");
        Animal a3 = new Animal(); a3.setTitle("Dog");

        a1 = rest.postForObject(baseUrl + "/animals", a1, Animal.class);
        a2 = rest.postForObject(baseUrl + "/animals", a2, Animal.class);
        a3 = rest.postForObject(baseUrl + "/animals", a3, Animal.class);

        assertThat(a1.getId()).isNotBlank();
        assertThat(a2.getId()).isNotBlank();
        assertThat(a3.getId()).isNotBlank();

        // Place a1 and a2 into green; a3 into big
        LocalDate today = LocalDate.now();
        rest.postForEntity(baseUrl + "/animals/" + a1.getId() + "/place?roomId=" + green.getId() + "&located=" + today, null, Animal.class);
        rest.postForEntity(baseUrl + "/animals/" + a2.getId() + "/place?roomId=" + green.getId() + "&located=" + today, null, Animal.class);
        rest.postForEntity(baseUrl + "/animals/" + a3.getId() + "/place?roomId=" + big.getId() + "&located=" + today, null, Animal.class);

        // Assign favorites
        rest.postForEntity(baseUrl + "/animals/" + a1.getId() + "/favorites/assign?roomId=" + green.getId(), null, Animal.class);
        rest.postForEntity(baseUrl + "/animals/" + a2.getId() + "/favorites/assign?roomId=" + green.getId(), null, Animal.class);
        rest.postForEntity(baseUrl + "/animals/" + a3.getId() + "/favorites/assign?roomId=" + big.getId(), null, Animal.class);

        // Small delay to avoid any eventual consistency in DynamoDB Local
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}

        // Sanity check: read back a1 and a2 after placement
        Animal a1AfterPlace = rest.getForObject(baseUrl + "/animals/" + a1.getId(), Animal.class);
        Animal a2AfterPlace = rest.getForObject(baseUrl + "/animals/" + a2.getId(), Animal.class);
        assertThat(a1AfterPlace.getRoomId()).isEqualTo(green.getId());
        assertThat(a2AfterPlace.getRoomId()).isEqualTo(green.getId());

        // Query animals in green, sorted by title desc, page 0 size 10
        ResponseEntity<Animal[]> inGreenResp = rest.getForEntity(baseUrl + "/animals/in-room/" + green.getId() + "?sortBy=title&order=desc&page=0&size=10", Animal[].class);
        assertThat(inGreenResp.getStatusCode().is2xxSuccessful()).isTrue();
        List<Animal> inGreen = java.util.Arrays.asList(inGreenResp.getBody());
        assertThat(inGreen).extracting(Animal::getTitle).containsExactlyInAnyOrder("Tiger", "Cat");

        // Favorites aggregation
        ResponseEntity<Map> favAggResp = rest.getForEntity(baseUrl + "/animals/favorites/aggregation", Map.class);
        assertThat(favAggResp.getStatusCode().is2xxSuccessful()).isTrue();
        Map<?,?> agg = favAggResp.getBody();
        assertThat(agg).isNotNull();
        assertThat(agg.get(green.getId())).isEqualTo(2);
        assertThat(agg.get(big.getId())).isEqualTo(1);

        // Move animal and remove
        rest.postForEntity(baseUrl + "/animals/" + a1.getId() + "/move?roomId=" + big.getId() + "&located=" + today, null, Animal.class);
        rest.postForEntity(baseUrl + "/animals/" + a2.getId() + "/remove", null, Animal.class);

        // Validate updates via GET
        Animal updatedA1 = rest.getForObject(baseUrl + "/animals/" + a1.getId(), Animal.class);
        Animal updatedA2 = rest.getForObject(baseUrl + "/animals/" + a2.getId(), Animal.class);
        assertThat(updatedA1.getRoomId()).isEqualTo(big.getId());
        assertThat(updatedA2.getRoomId()).isNull();

        // Cleanup: delete a3
        ResponseEntity<Void> del = rest.exchange(baseUrl + "/animals/" + a3.getId(), HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
        assertThat(del.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
