
package org.alsception.pegasus.features.notifications;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository repository;
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    public NotificationController(NotificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Page<PGSNotification> getNotifications(Authentication auth, Pageable pageable) {
        return repository.findByToOrderByCreatedDesc(auth.getName(), pageable);
    }

    @GetMapping("/unread-count")
    public long getUnreadCount(Authentication auth) {
        return repository.countByToAndReadFalse(auth.getName());
    }

    @GetMapping("/unread")
    public List<PGSNotification> getUnread(Authentication auth) {
        return repository.findByToAndReadFalseOrderByCreatedDesc(auth.getName());
    }

    @PatchMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id, Authentication auth) {
        PGSNotification notification = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getTo().equals(auth.getName())) {
            throw new RuntimeException("Not your notification");
        }

        notification.setRead(true);
        repository.save(notification);
    }
}