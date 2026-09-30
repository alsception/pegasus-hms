package org.alsception.pegasus.features.reservations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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

    @Query("""
    select r
    from PGSReservation r
    join fetch r.room
    where (CAST(:checkIn AS date) IS NULL OR r.checkIn >= :checkIn)
      and (CAST(:checkOut AS date) IS NULL OR r.checkOut <= :checkOut)
      and (:priceFrom IS NULL OR r.totalPrice >= :priceFrom)
      and (:priceTo   IS NULL OR r.totalPrice <= :priceTo)
      and (:guests    IS NULL OR r.guests = :guests)
    order by r.checkIn asc, r.created desc 
    """)
    List<PGSReservation> search(@Param("checkIn") LocalDate checkIn,
                            @Param("checkOut") LocalDate checkOut,
                            @Param("priceFrom") BigDecimal priceFrom,
                            @Param("priceTo") BigDecimal priceTo,
                            @Param("guests") Integer guests);
}