package org.alsception.pegasus.features.reservations;

import jakarta.persistence.*;
import lombok.*;
import org.alsception.pegasus.features.rooms.PGSRoom;
import org.alsception.pegasus.features.users.PGSUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@Entity
@Table(
    name = "pgs_reservations",
    indexes = {
        @Index(name = "idx_reservation_room", columnList = "room_id"),
        @Index(name = "idx_reservation_guest", columnList = "guest_id"),
        @Index(name = "idx_reservation_check_in", columnList = "check_in"),
        @Index(name = "idx_reservation_check_out", columnList = "check_out")
    }
)
public class PGSReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Guest who made the reservation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booker_id")
    @ToString.Exclude
    private PGSUser booker;

    /**
     * Reserved room.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    @ToString.Exclude
    private PGSRoom room;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(name = "expected_arrival_time")
    private LocalTime expectedArrivalTime;

    @Column(name = "expected_departure_time")
    private LocalTime expectedDepartureTime;

    @Column(nullable = false)
    private Integer guests = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PGSReservationStatus status = PGSReservationStatus.PENDING;

    /**
     * Price agreed at the time of reservation.
     * This should not change if the room price changes later.
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created", updatable = false)
    private LocalDateTime created;

    @Column(name = "modified")
    private LocalDateTime modified;

    @PrePersist
    protected void onCreate() {
        created = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        modified = LocalDateTime.now();
    }
}