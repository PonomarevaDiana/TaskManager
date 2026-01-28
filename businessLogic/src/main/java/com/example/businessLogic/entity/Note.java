package com.example.businessLogic.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notes", indexes = {
        @Index(name = "idx_notes_object_id_is_project",
                columnList = "object_id, is_project"),
        @Index(name = "idx_notes_user_id_created_at",
                columnList = "user_id, created_at DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "object_id", nullable = false)
    private String objectId;

    @Column(name = "is_project", nullable = false)
    private Boolean isProject;

    @Column(name = "use_markdown")
    private Boolean useMarkdown = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}