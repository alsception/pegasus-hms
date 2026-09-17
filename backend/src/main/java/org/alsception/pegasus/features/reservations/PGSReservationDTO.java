package org.alsception.pegasus.features.reservations;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class PGSReservationDTO {

    private Long id;

    private Long bookerId;

    private Long roomId;

    private LocalDate checkIn;

    private LocalDate checkOut;

    private LocalTime expectedArrivalTime;

    private LocalTime expectedDepartureTime;

    private PGSReservationStatus status;

    private BigDecimal totalPrice;

    private String notes;
}