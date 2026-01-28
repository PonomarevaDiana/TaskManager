package com.example.businessLogic.repository;

import com.example.businessLogic.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, String> {
    List<ProjectMember> findByUserId(String projectId);

    List<ProjectMember> findByProjectId(String projectId);

    boolean existsByProjectIdAndUserId(String projectId, String userId);

    Optional<ProjectMember> findByProjectIdAndUserId(String projectId, String userId);
}