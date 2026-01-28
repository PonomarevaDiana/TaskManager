package com.example.businessLogic.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "contact_requests", indexes = {
        @Index(name = "idx_contact_requests_recipient", columnList = "recipient"),
        @Index(name = "idx_contact_requests_owner", columnList = "owner_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_contact_requests_owner_recipient", columnNames = {"owner_id", "recipient"})
})
@Data
public class ContactRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id")
    private String owner;

    @Column(name = "recipient")
    private String recipient;

    @Column(name = "create_at")
    private String create_at;
}