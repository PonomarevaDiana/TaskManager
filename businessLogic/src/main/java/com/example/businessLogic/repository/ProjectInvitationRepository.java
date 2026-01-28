package com.example.businessLogic.repository;

import com.example.businessLogic.entity.InvitationStatus;
import com.example.businessLogic.entity.ProjectInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, Long> {

    List<ProjectInvitation> findByInvitedUserIdAndStatus(String invitedUserId, InvitationStatus status);

    Optional<ProjectInvitation> findByProjectIdAndInvitedUserIdAndStatus(String projectId,
                                                                         String invitedUserId,
                                                                         InvitationStatus status);

    boolean existsByProjectIdAndInvitedUserIdAndStatus(String projectId,
                                                       String invitedUserId,
                                                       InvitationStatus status);
}
