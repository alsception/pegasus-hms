package org.alsception.pegasus.features.notifications;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pgs_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PGSNotification 
{
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column()
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "from_user")
    private String from;
    
    @Column(name = "to_user")
    private String to;

    @Column(nullable = false)
    private Boolean read = false;

    /* @Enumerated(EnumType.STRING) */
    @Column(length = 20)
    private String type;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_type", length = 30)
    private String referenceType;
    
    @CreationTimestamp
    @Column(name = "created", nullable = true, updatable = false)
    private LocalDateTime created;

    @Column(nullable = true)
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