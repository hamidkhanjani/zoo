package com.eurail.zooeurail.repository;

import com.eurail.zooeurail.model.Animal;
import com.eurail.zooeurail.model.Room;
import com.eurail.zooeurail.support.DynamoDbTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "aws.dynamodb.createTablesOnStartup=true"
})
public class AnimalRepositoryIT extends DynamoDbTestBase {

    @Autowired
    AnimalRepository animalRepository;

    @Autowired
    RoomRepository roomRepository;

    Room green;
    Room blue;

    @BeforeEach
    void init() {
        // create two rooms
        green = new Room();
        green.setTitle("Green");
        green = roomRepository.save(green);
        assertThat(green.getId()).isNotBlank();

        blue = new Room();
        blue.setTitle("Blue");
        blue = roomRepository.save(blue);
        assertThat(blue.getId()).isNotBlank();
    }

    @Test
    void save_and_find_room() {
        // create animals
        Animal a1 = new Animal();
        a1.setTitle("Tiger");
        Animal a2 = new Animal();
        a2.setTitle("Cat");
        Animal a3 = new Animal();
        a3.setTitle("Dog");

        a1 = animalRepository.save(a1);
        a2 = animalRepository.save(a2);
        a3 = animalRepository.save(a3);

        assertThat(a1.getId()).isNotBlank();
        assertThat(a2.getId()).isNotBlank();
        assertThat(a3.getId()).isNotBlank();

        // findById
        Optional<Animal> foundA1 = animalRepository.findById(a1.getId());
        assertThat(foundA1).isPresent();
        assertThat(foundA1.get().getTitle()).isEqualTo("Tiger");

        // basic scan should at least return these animals
        List<Animal> all = animalRepository.findAll();
        assertThat(all).extracting(Animal::getId)
                .contains(a1.getId(), a2.getId(), a3.getId());
    }

    @Test
    void update_room() {
        // create animals
        Animal a1 = new Animal();
        a1.setTitle("Tiger");
        Animal a2 = new Animal();
        a2.setTitle("Cat");
        Animal a3 = new Animal();
        a3.setTitle("Dog");

        a1 = animalRepository.save(a1);
        a2 = animalRepository.save(a2);
        a3 = animalRepository.save(a3);

        // update: place a1 and a2 in green, a3 in blue
        LocalDate today = LocalDate.now();
        a1.setRoomId(green.getId());
        a1.setLocated(today);
        a2.setRoomId(green.getId());
        a2.setLocated(today);
        a3.setRoomId(blue.getId());
        a3.setLocated(today);
        animalRepository.save(a1);
        animalRepository.save(a2);
        animalRepository.save(a3);

        // verify updates persisted
        assertThat(animalRepository.findById(a1.getId()).map(Animal::getRoomId)).contains(green.getId());
        assertThat(animalRepository.findById(a2.getId()).map(Animal::getRoomId)).contains(green.getId());
        assertThat(animalRepository.findById(a3.getId()).map(Animal::getRoomId)).contains(blue.getId());
    }

    @Test
    void delete_room() {
        // create animals
        Animal a1 = new Animal();
        a1.setTitle("Tiger");
        Animal a2 = new Animal();
        a2.setTitle("Cat");
        Animal a3 = new Animal();
        a3.setTitle("Dog");

        a1 = animalRepository.save(a1);
        a2 = animalRepository.save(a2);
        a3 = animalRepository.save(a3);

        // delete one
        animalRepository.deleteById(a2.getId());
        assertThat(animalRepository.findById(a2.getId())).isEmpty();

        // remaining scan should still include a1 and a3
        List<Animal> afterDelete = animalRepository.findAll();
        assertThat(afterDelete).extracting(Animal::getId)
                .contains(a1.getId(), a3.getId())
                .doesNotContain(a2.getId());
    }
}