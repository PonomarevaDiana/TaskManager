package com.example.businessLogic.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Entity
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name="projects", indexes = {
        @Index(name = "idx_projects_created_at", columnList = "created_at DESC"),
        @Index(name = "idx_projects_auto_delete", columnList = "auto_delete_days")
})
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private String id;

    private String name;
    private String description;
    private LocalDate createdAt;
    private Integer autoDeleteDays;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDate.now();
        }
        if (autoDeleteDays == null) {
            autoDeleteDays = 7;
        }
    }
}