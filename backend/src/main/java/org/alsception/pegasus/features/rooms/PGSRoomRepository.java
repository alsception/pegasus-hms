package org.alsception.pegasus.features.rooms;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PGSRoomRepository extends JpaRepository<PGSRoom, Long> {

    Optional<PGSRoom> findByRoomNumber(String roomNumber);   

    boolean existsByRoomNumber(String roomNumber);

    /*List<PGSRoom> findAllByOrderByRoomNumberAsc();
    List<PGSRoom> findAllByOrderByRoomNumberDesc();*/
}