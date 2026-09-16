package org.alsception.pegasus.features.rooms;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class PGSRoomController {

    private final PGSRoomService roomService;

    @GetMapping
    public ResponseEntity<List<PGSRoom>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PGSRoom> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    @GetMapping("/number/{roomNumber}")
    public ResponseEntity<PGSRoom> getRoomByNumber(
            @PathVariable String roomNumber
    ) {
        return ResponseEntity.ok(roomService.getRoomByNumber(roomNumber));
    }

    @PostMapping
    public ResponseEntity<PGSRoom> createRoom(
            @RequestBody PGSRoom room
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(roomService.createRoom(room));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PGSRoom> updateRoom(
            @PathVariable Long id,
            @RequestBody PGSRoom room
    ) {
        return ResponseEntity.ok(roomService.updateRoom(id, room));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}