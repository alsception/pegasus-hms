package org.alsception.pegasus.features.rooms;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PGSRoomService {

    private final PGSRoomRepository roomRepository;

    public List<PGSRoom> getAllRooms() {
        return roomRepository.findAll();
    }

    public PGSRoom getRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Room not found with id: " + id));
    }

    public PGSRoom getRoomByNumber(String roomNumber) {
        return roomRepository.findByRoomNumber(roomNumber)
                .orElseThrow(() ->
                        new RuntimeException("Room not found: " + roomNumber));
    }

    public PGSRoom createRoom(PGSRoom room) {

        if (roomRepository.existsByRoomNumber(room.getRoomNumber())) {
            throw new RuntimeException(
                    "Room already exists: " + room.getRoomNumber()
            );
        }

        return roomRepository.save(room);
    }

    public PGSRoom updateRoom(Long id, PGSRoom updatedRoom) {

        PGSRoom room = getRoomById(id);

        room.setRoomNumber(updatedRoom.getRoomNumber());
        room.setType(updatedRoom.getType());
        room.setStatus(updatedRoom.getStatus());
        room.setFloor(updatedRoom.getFloor());
        room.setCapacity(updatedRoom.getCapacity());
        room.setPricePerNight(updatedRoom.getPricePerNight());
        room.setDescription(updatedRoom.getDescription());
        room.setActive(updatedRoom.getActive());

        return roomRepository.save(room);
    }

    public void deleteRoom(Long id) {
        PGSRoom room = getRoomById(id);
        roomRepository.delete(room);
    }
}