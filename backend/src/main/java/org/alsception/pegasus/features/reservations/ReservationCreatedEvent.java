package org.alsception.pegasus.features.events;

public record ReservationCreatedEvent(
        Long reservationId,
        String username) {
}