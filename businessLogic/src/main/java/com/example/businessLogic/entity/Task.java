package com.example.businessLogic.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "tasks", indexes={
        @Index(name = "idx_tasks_title", columnList = "task_title"),
        @Index(name = "idx_tasks_creator_id", columnList = "creator_id"),
        @Index(name = "idx_tasks_project_id", columnList = "project_id"),
        @Index(name = "idx_tasks_create_date", columnList = "create_date"),
        @Index(name = "idx_tasks_deadline_date", columnList = "deadline_date"),
        @Index(name = "idx_tasks_completion_date", columnList = "completion_date"),
        @Index(name = "idx_tasks_status_id", columnList = "status_id"),
        @Index(name = "idx_tasks_priority_id", columnList = "priority_id"),
        @Index(name = "idx_tasks_project_completion", columnList = "project_id, completion_date")

})
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "task_id", updatable = false, nullable = false)
    private String id;

    @Column(name = "task_title", nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name="project_id", nullable = true)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "task_assignees",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"),
            indexes = {
                    @Index(name = "idx_task_assignees_task_id", columnList = "task_id"),
                    @Index(name = "idx_task_assignees_user_id", columnList = "user_id")
            }
    )
    private Set<User> assignees = new HashSet<>();

    @Column(name = "create_date", nullable = false)
    private LocalDate createDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "deadline_date")
    private LocalDate deadlineDate;

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "priority_id")
    private Priority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id")
    private Status status;

    // PrePersist для автоматической установки даты создания
    @PrePersist
    protected void onCreate() {
        if (createDate == null) {
            createDate = LocalDate.now();
        }
    }

    public boolean isPersonal() {
        return project == null;
    }
}