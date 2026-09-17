package org.alsception.pegasus.features.rooms;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(
    name = "pgs_rooms",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_room_number",
            columnNames = "room_number"
        )
    }
)
public class PGSRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_number", nullable = false, unique = true)
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PGSRoomType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PGSRoomStatus status = PGSRoomStatus.AVAILABLE;

    @Column(nullable = false)
    private Integer floor;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    @Column
    private String description;

    @Column(length = 255)
    private String imageUrl;

    @Column(nullable = false)
    private Boolean active = true;

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

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}