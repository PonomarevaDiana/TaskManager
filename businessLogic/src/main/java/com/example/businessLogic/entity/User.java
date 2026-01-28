package com.example.businessLogic.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "users", indexes = {
        @Index(name="idx_users_username", columnList="user_name"),
        @Index(name="idx_friendship_key", columnList ="friendship_key"),
        @Index(name="idx_friendship_key_username", columnList = "friendship_key, user_name")
})
public class User {
    @Id
    @Column(name="user_id", nullable = false)
    @EqualsAndHashCode.Include
    private String id;

    @Column(name="user_name", nullable = false)
    private String username;

    @Column(name="email")
    private String email;

    @Column(name="first_name")
    private String firstName;

    @Column(name="last_name")
    private String lastName;

    @Column(name="friendship_key", nullable = false)
    private String friendshipKey;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @ToString.Exclude
    private Set<ProjectMember> projectMembership = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"),
            indexes = @Index(name = "idx_user_roles_user_id", columnList = "user_id"))
    private Set<String> roles;

    @PrePersist
    @PreUpdate
    public void generateFriendshipKey() {
        if (this.friendshipKey == null) {
            this.friendshipKey = java.util.UUID.randomUUID().toString().substring(0, 8);
        }
    }
}