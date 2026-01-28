package com.example.businessLogic.id;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectMemberId implements Serializable {
    @Column(name = "user_id")
    private String user;

    @Column(name = "project_id")
    private String project;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectMemberId that)) return false;
        return Objects.equals(user, that.user) && Objects.equals(project, that.project);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, project);
    }
}
