package org.alsception.pegasus.features.events;

import lombok.RequiredArgsConstructor;
import org.alsception.pegasus.features.notifications.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ReservationNotificationListener
{
    private final NotificationService notificationService;
    private static final Logger logger = LoggerFactory.getLogger(ReservationNotificationListener.class);

    /**
     * This listener runs only after the reservation transaction
     * has been successfully committed.
     *
     * This prevents notifications from being created for
     * reservations that were rolled back.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservationCreated(ReservationCreatedEvent event)
    {
        logger.info("Captured new reservation created event");

        logger.info("Calling notificationService.createNotification()");

        notificationService.createNotification(
                "Nova rezervacija",
                "Kreirana je nova rezervacija.",
                "SYSTEM",
                event.username(),
                "RESERVATION_CREATED",
                "PGSReservation",
                event.reservationId()
        );

        logger.info("Returned from notificationService.createNotification()");
    }
}