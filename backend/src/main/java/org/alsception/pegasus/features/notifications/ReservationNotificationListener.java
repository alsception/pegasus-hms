package org.alsception.pegasus.features.notifications;

import org.alsception.pegasus.features.reservations.ReservationCreatedEvent;
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
     * 
     * Ovo je event listener. Kad se kreira rezervacija, negdje se objavi ReservationCreatedEvent. 
     * Listener ga hvata s @TransactionalEventListener(phase = AFTER_COMMIT), što znači da se izvršava tek nakon što je transakcija uspješno commitana. 
     * Ako se rezervacija rollbacka, notifikacija se ne kreira. Zatim poziva notificationService.createNotification(...) 
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservationCreated(ReservationCreatedEvent event)
    {
        logger.debug("Captured new reservation created event, id["+event.reservationId() + "], username["+event.username()+"]");

        //1. This method saves to database
        PGSNotification notification = notificationService.createNewReservationNotification(event.username(), event.reservationId());
        
        //2. This method send to websocket
        notificationService.sendToUser(event.username(), notification);
    }
}