package org.alsception.pegasus.features.reservations;

public record ReservationCreatedEvent( Long reservationId, String username ) {}