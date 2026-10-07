package org.alsception.pegasus.features.notifications;

import lombok.RequiredArgsConstructor;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import org.alsception.pegasus.features.order.PGSOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    
    // Get all notifications for a user
    public List<PGSNotification> getNotificationsForUserTo(String username) {
        return notificationRepository.findByToAndCreatedGreaterThanEqualOrderByCreatedDesc(username, LocalDateTime.now().minusMinutes(15));
    }

    // Get all notifications for a user
    public List<PGSNotification> getNotificationsForUserFrom(String username) {
        return notificationRepository.findByFromOrderByCreatedDesc(username);
    }
    
    // Get unread notifications for a user
    public List<PGSNotification> getUnreadNotifications(String username) {
        return notificationRepository.findByToAndReadFalseOrderByCreatedDesc(username);
    }
    
    // Get read notifications for a user
    public List<PGSNotification> getReadNotifications(String username) {
        return notificationRepository.findByToAndReadTrueOrderByCreatedDesc(username);
    }
    
    // Get notifications by type
    public List<PGSNotification> getNotificationsByType(String username, String type) {
        return notificationRepository.findByToAndTypeOrderByCreatedDesc(username, type);
    }
    
    // Get unread count
    public Long getUnreadCount(String username) {
        return notificationRepository.countByToAndReadFalse(username);
    }
    
    // Create a new notification
    @Transactional
    public PGSNotification createNotification(PGSNotification notification) {
        return notificationRepository.save(notification);
    }
    
    // Create notification with builder pattern
    @Transactional(propagation = Propagation.REQUIRES_NEW)  //Ovo je bitno
    public void createNotification(String title, String message, String from, String to, String type, String refType, Long refId) 
    {
        System.out.println("asfasdasdsadasd");
        logger.debug("Creating notification");
        PGSNotification notification = new PGSNotification();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setFrom(from);
        notification.setTo(to);
        notification.setType(type);
        notification.setReferenceType(refType);
        notification.setReferenceId(refId);
        System.out.println("savin notifications...");
        System.out.println(notification.toString());
        notification = notificationRepository.save(notification);
        System.out.println("Notification saved "+notification.getId());


        messagingTemplate.convertAndSendToUser(
                to,                        // username primatelja
                "/queue/notifications",    // destinacija
                notification               // bolje DTO nego entitet
        );
    }
    
    // Mark notification as read
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.markAsRead(notificationId);
    }
    
    // Mark all notifications as read for a user
    @Transactional
    public void markAllAsRead(String username) {
        notificationRepository.markAllAsReadForUser(username);
    }
    
    // Delete a notification
    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }
    
    // Delete all read notifications for a user
    @Transactional
    public void deleteReadNotifications(String username) {
        notificationRepository.deleteByToAndReadTrue(username);
    }
    
    // Get notification by ID
    public PGSNotification getNotificationById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + id));
    }

    /**
     * TODO: 
     * isBlank će da uzrokuje error ako je stol null:
     * Error updating order: Cannot invoke "String.isBlank()" because the return value of "org.alsception.pegasus.features.order.PGSOrder.getStol()" is null
        java.lang.NullPointerException: Cannot invoke "String.isBlank()" because the return value of "org.alsception.pegasus.features.order.PGSOrder.getStol()" is null
	    at org.alsception.pegasus.features.notifications.NotificationService.createOrderStatusText(NotificationService.java:132)
     */

    public String createNewOrderText(PGSOrder order)
    {
        String text = "Nova narudžba <b>"+order.getCode()+"</b>, korisnik <b>"+order.getUser().getUsername()+"</b>";
        return text;
    }

    public String createOrderReadyText(PGSOrder order)
    {
        String text = "Narudžba <b>"+order.getCode()+"</b> je spremna";
        return text;
    }
    
    public String createOrderInprepText(PGSOrder order)
    {
        String text = "Narudžba <b>"+order.getCode()+"</b> je u pripremi";
        return text;
    }

    public String createOrderServedText(PGSOrder order)
    {
        String text = "Narudžba <b>"+order.getCode()+"</b> je dostavljena";
        return text;
    }
    
    public String createOrderStatusText(PGSOrder order)
    {
        String text = "Narudžba <b>"+order.getCode()+"</b> je: <b>"+order.getStatus()+"</b>";
        return text;
    }
}