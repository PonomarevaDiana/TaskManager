package com.example.businessLogic.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "contacts", indexes={@Index(name = "idx_contacts_owner_id", columnList = "owner_id"),
        @Index(name = "idx_contacts_id_owner", columnList = "id, owner_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_contacts_owner_contact", columnNames = {"owner_id", "contact_user_id"})
}
)
@Data
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne
    @JoinColumn(name = "contact_user_id")
    private User contactUser;

    @Column(name = "is_pinned")
    private Boolean isPinned = false;

}