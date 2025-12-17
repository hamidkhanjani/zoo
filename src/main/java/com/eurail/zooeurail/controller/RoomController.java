package com.eurail.zooeurail.controller;

import com.eurail.zooeurail.model.Room;
import com.eurail.zooeurail.service.RoomService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Rooms", description = "Endpoints for managing rooms")
@Slf4j
public class RoomController extends BaseController<Room> {

    public RoomController(RoomService service) {
        super(service);
    }
}
