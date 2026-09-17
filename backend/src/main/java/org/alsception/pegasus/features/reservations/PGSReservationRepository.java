package org.alsception.pegasus.features.reservations;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PGSReservationRepository
        extends JpaRepository<PGSReservation, Long> {

    List<PGSReservation> findAllByOrderByCheckInAsc();

    List<PGSReservation> findByRoomIdOrderByCheckInAsc(Long roomId);

    List<PGSReservation> findByBookerIdOrderByCheckInDesc(Long userId);

    boolean existsByRoomIdAndCheckInLessThanAndCheckOutGreaterThanAndStatusNot(
        Long roomId,
        LocalDate checkOut,
        LocalDate checkIn,
        PGSReservationStatus status
    );
}