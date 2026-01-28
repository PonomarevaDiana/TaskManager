package com.example.businessLogic.service;

import com.example.businessLogic.dto.NotificationRequest;
import com.example.businessLogic.entity.*;
import com.example.businessLogic.id.ProjectMemberId;
import com.example.businessLogic.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class ProjectInvitationService {

    private final NotificationRepository notificationRepository;
    private ProjectInvitationRepository invitationRepository;
    private ProjectRepository projectRepository;
    private UserRepository userRepository;
    private ProjectMemberRepository projectMemberRepository;
    private NotificationService notificationService;

    @Transactional
    public ProjectInvitation sendInvitation(String projectId, String invitedUserId, String inviterUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        userRepository.findById(invitedUserId)
                .orElseThrow(() -> new EntityNotFoundException("invited user not found"));

        User inviterUser = userRepository.findById(inviterUserId)
                .orElseThrow(() -> new EntityNotFoundException("inviter not found"));

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, invitedUserId)) {
            throw new IllegalStateException("Пользователь уже участник проекта");
        }

        if (invitationRepository.existsByProjectIdAndInvitedUserIdAndStatus(
                projectId, invitedUserId, InvitationStatus.PENDING)) {
            throw new IllegalStateException("Приглашение уже отправлено этому пользователю");
        }

        ProjectInvitation projectInvitation = ProjectInvitation.builder()
                .projectId(projectId)
                .invitedUserId(invitedUserId)
                .inviterUserId(inviterUserId)
                .status(InvitationStatus.PENDING)
                .build();

        ProjectInvitation saved = invitationRepository.save(projectInvitation);
        sendInvitationNotification(project, inviterUser, saved);

        return saved;
    }

    @Transactional
    public void acceptInvitation(Long invitationId, String invitedUserId) {
        ProjectInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new EntityNotFoundException("Invitation not found"));

        if (!invitation.getInvitedUserId().equals(invitedUserId)) {
            throw new IllegalArgumentException("Это приглашение для другого пользователя");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalStateException("Приглашение уже обработано");
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        Project project = projectRepository.findById(invitation.getProjectId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        User invitedUser = userRepository.findById(invitedUserId)
                .orElseThrow(() -> new EntityNotFoundException("invited user not found"));


        addProjectMember(project, invitedUser);

        deleteInvitationNotification(invitation.getId());

        notifyInviterAboutResponse(invitation, true);
    }

    @Transactional
    public void declineInvitation(Long invitationId, String invitedUserId) {
        ProjectInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new EntityNotFoundException("Invitation not found"));

        if (!invitation.getInvitedUserId().equals(invitedUserId)) {
            throw new IllegalArgumentException("Это приглашение для другого пользователя");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalStateException("Приглашение уже обработано");
        }

        invitation.setStatus(InvitationStatus.DECLINED);
        invitation.setRespondedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        deleteInvitationNotification(invitation.getId());

        notifyInviterAboutResponse(invitation, false);
    }

    public List<ProjectInvitation> getPendingInvitations(String userId) {
        return invitationRepository.findByInvitedUserIdAndStatus(userId, InvitationStatus.PENDING);
    }

    private void addProjectMember(Project project, User user) {
        ProjectMemberId memberId = new ProjectMemberId();
        memberId.setProject(project.getId());
        memberId.setUser(user.getId());

        ProjectMember member = new ProjectMember();
        member.setId(memberId);
        member.setUser(user);
        member.setProject(project);
        member.setRole(Role.ROLE_USER);

        projectMemberRepository.save(member);
    }

    private void sendInvitationNotification(Project project, User inviterUser, ProjectInvitation invitation) {
        NotificationRequest notification = NotificationRequest.builder()
                .title("Приглашение в проект")
                .message(inviterUser.getUsername() + " приглашает вас в проект \"" + project.getName() + "\"")
                .senderId(inviterUser.getId())
                .type(NotificationType.PROJECT_INVITATION)
                .invitationId(invitation.getId())
                .build();

        notificationService.sendNotification(invitation.getInvitedUserId(), notification);
    }

    private void notifyInviterAboutResponse(ProjectInvitation invitation, boolean accepted) {
        User user = userRepository.findById(invitation.getInvitedUserId())
                .orElseThrow(() -> new EntityNotFoundException("user not found"));
        Project project = projectRepository.findById(invitation.getProjectId())
                .orElseThrow(() -> new EntityNotFoundException("project not found"));

        String message = accepted
                ? user.getUsername() + " принял приглащение в проект \"" + project.getName()
                : user.getUsername() + " отклонил приглашение в проект \"" + project.getName();

        NotificationRequest notification = NotificationRequest.builder()
                .title("Ответ на приглашение")
                .message(message)
                .senderId(invitation.getInviterUserId())
                .type(NotificationType.SYSTEM_ALERT)
                .build();

        notificationService.sendNotification(invitation.getInviterUserId(), notification);
    }

    private void deleteInvitationNotification(Long invitationId) {
        Long notificationId = notificationRepository.findIdByInvitationId(invitationId);
        if (notificationId != null) {
            notificationRepository.deleteById(notificationId);
        }
    }
}
