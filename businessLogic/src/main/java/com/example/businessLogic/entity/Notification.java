package com.example.businessLogic.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_owner_date_read",
                columnList = "owner_id, date DESC, is_read"),
        @Index(name = "idx_notifications_owner_type",
                columnList = "owner_id, type"),
        @Index(name = "idx_notifications_invitation_id",
                columnList = "invitation_id", unique = true),
        @Index(name = "idx_notifications_owner_date",
                columnList = "owner_id, date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id")
    private String ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private NotificationType type;

    @Column(name = "title")
    private String title;

    @Column(name = "date")
    private LocalDateTime date;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "message")
    private String message;

    @Column(name = "sender_id")
    private String senderId;

    @Column(name = "invitation_id")
    private Long invitationId;

    @PrePersist
    protected void onCreate() {
        this.date = LocalDateTime.now();
    }
}