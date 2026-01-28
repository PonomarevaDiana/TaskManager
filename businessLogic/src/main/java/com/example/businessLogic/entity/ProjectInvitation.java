package com.example.businessLogic.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity(name="project_invitations")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "project_invitations", indexes = {
        @Index(name = "idx_project_invitations_invited_user_status", columnList = "invited_user_id, status"),
        @Index(name = "idx_project_invitations_project_invited_status", columnList = "project_id, invited_user_id, status")
})
public class ProjectInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String projectId;

    @Column(nullable = false)
    private String invitedUserId;

    @Column(nullable = false)
    private String inviterUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime respondedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
