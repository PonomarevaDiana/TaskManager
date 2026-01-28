package com.example.businessLogic.repository;

import com.example.businessLogic.entity.Project;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
    Project getReferenceById(@NonNull String id);

    @Query("select distinct p from Project p join ProjectMember pm on p.id = pm.project.id where pm.user.id = :userId")
    List<Project> findAllByUserId(@Param("userId") String userId);
}
