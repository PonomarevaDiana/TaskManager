package com.example.businessLogic.entity;

import com.example.businessLogic.id.ProjectMemberId;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name="project_member",indexes = {
        @Index(name = "idx_project_member_user_id", columnList = "user_id"),
        @Index(name = "idx_project_member_project_id", columnList = "project_id"),
        @Index(name = "idx_project_member_project_user", columnList = "project_id, user_id")
})
@Data
public class ProjectMember {

    @EmbeddedId
    private ProjectMemberId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("user")
    @JoinColumn(name="user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("project")
    @JoinColumn(name="project_id")
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

}