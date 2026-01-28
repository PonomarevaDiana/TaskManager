package com.example.businessLogic.service;

import com.example.businessLogic.dto.NotificationRequest;
import com.example.businessLogic.entity.*;
import com.example.businessLogic.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectInvitationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ProjectInvitationRepository invitationRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ProjectInvitationService projectInvitationService;

    @Test
    void sendInvitation_WithValidData_ShouldCreateInvitation() {
        String projectId = "project1";
        String invitedUserId = "user2";
        String inviterUserId = "user1";

        Project project = new Project();
        project.setId(projectId);
        project.setName("Test Project");

        User invitedUser = new User();
        invitedUser.setId(invitedUserId);

        User inviterUser = new User();
        inviterUser.setId(inviterUserId);
        inviterUser.setUsername("InviterUser");

        ProjectInvitation invitation = ProjectInvitation.builder()
                .id(1L)
                .projectId(projectId)
                .invitedUserId(invitedUserId)
                .inviterUserId(inviterUserId)
                .status(InvitationStatus.PENDING)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(invitedUserId)).thenReturn(Optional.of(invitedUser));
        when(userRepository.findById(inviterUserId)).thenReturn(Optional.of(inviterUser));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, invitedUserId)).thenReturn(false);
        when(invitationRepository.existsByProjectIdAndInvitedUserIdAndStatus(projectId, invitedUserId, InvitationStatus.PENDING)).thenReturn(false);
        when(invitationRepository.save(any(ProjectInvitation.class))).thenReturn(invitation);

        ProjectInvitation result = projectInvitationService.sendInvitation(projectId, invitedUserId, inviterUserId);

        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        assertEquals(invitedUserId, result.getInvitedUserId());
        assertEquals(inviterUserId, result.getInviterUserId());
        assertEquals(InvitationStatus.PENDING, result.getStatus());

        verify(invitationRepository).save(any(ProjectInvitation.class));
        verify(notificationService).sendNotification(eq(invitedUserId), any(NotificationRequest.class));
    }

    @Test
    void sendInvitation_WhenProjectNotFound_ShouldThrowException() {
        String projectId = "non-existent-project";
        String invitedUserId = "user2";
        String inviterUserId = "user1";

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> projectInvitationService.sendInvitation(projectId, invitedUserId, inviterUserId));
        assertEquals("Project not found", exception.getMessage());

        verify(invitationRepository, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void sendInvitation_WhenInvitedUserNotFound_ShouldThrowException() {
        String projectId = "project1";
        String invitedUserId = "non-existent-user";
        String inviterUserId = "user1";

        Project project = new Project();
        project.setId(projectId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(invitedUserId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> projectInvitationService.sendInvitation(projectId, invitedUserId, inviterUserId));
        assertEquals("invited user not found", exception.getMessage());

        verify(invitationRepository, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void sendInvitation_WhenInviterUserNotFound_ShouldThrowException() {
        String projectId = "project1";
        String invitedUserId = "user2";
        String inviterUserId = "non-existent-user";

        Project project = new Project();
        project.setId(projectId);

        User invitedUser = new User();
        invitedUser.setId(invitedUserId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(invitedUserId)).thenReturn(Optional.of(invitedUser));
        when(userRepository.findById(inviterUserId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> projectInvitationService.sendInvitation(projectId, invitedUserId, inviterUserId));
        assertEquals("inviter not found", exception.getMessage());

        verify(invitationRepository, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void sendInvitation_WhenUserAlreadyProjectMember_ShouldThrowException() {
        String projectId = "project1";
        String invitedUserId = "user2";
        String inviterUserId = "user1";

        Project project = new Project();
        project.setId(projectId);

        User invitedUser = new User();
        invitedUser.setId(invitedUserId);

        User inviterUser = new User();
        inviterUser.setId(inviterUserId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(invitedUserId)).thenReturn(Optional.of(invitedUser));
        when(userRepository.findById(inviterUserId)).thenReturn(Optional.of(inviterUser));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, invitedUserId)).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> projectInvitationService.sendInvitation(projectId, invitedUserId, inviterUserId));
        assertEquals("Пользователь уже участник проекта", exception.getMessage());

        verify(invitationRepository, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void sendInvitation_WhenInvitationAlreadyExists_ShouldThrowException() {
        String projectId = "project1";
        String invitedUserId = "user2";
        String inviterUserId = "user1";

        Project project = new Project();
        project.setId(projectId);

        User invitedUser = new User();
        invitedUser.setId(invitedUserId);

        User inviterUser = new User();
        inviterUser.setId(inviterUserId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(invitedUserId)).thenReturn(Optional.of(invitedUser));
        when(userRepository.findById(inviterUserId)).thenReturn(Optional.of(inviterUser));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, invitedUserId)).thenReturn(false);
        when(invitationRepository.existsByProjectIdAndInvitedUserIdAndStatus(projectId, invitedUserId, InvitationStatus.PENDING)).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> projectInvitationService.sendInvitation(projectId, invitedUserId, inviterUserId));
        assertEquals("Приглашение уже отправлено этому пользователю", exception.getMessage());

        verify(invitationRepository, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void acceptInvitation_WhenInvitationNotFound_ShouldThrowException() {
        Long invitationId = 1L;
        String invitedUserId = "user2";

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> projectInvitationService.acceptInvitation(invitationId, invitedUserId));
        assertEquals("Invitation not found", exception.getMessage());

        verify(invitationRepository, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void acceptInvitation_WhenWrongUser_ShouldThrowException() {
        Long invitationId = 1L;
        String invitedUserId = "user2";
        String wrongUserId = "user3";

        ProjectInvitation invitation = ProjectInvitation.builder()
                .id(invitationId)
                .projectId("project1")
                .invitedUserId(invitedUserId)
                .inviterUserId("user1")
                .status(InvitationStatus.PENDING)
                .build();

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> projectInvitationService.acceptInvitation(invitationId, wrongUserId));
        assertEquals("Это приглашение для другого пользователя", exception.getMessage());

        verify(invitationRepository, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void acceptInvitation_WhenAlreadyProcessed_ShouldThrowException() {
        Long invitationId = 1L;
        String invitedUserId = "user2";

        ProjectInvitation invitation = ProjectInvitation.builder()
                .id(invitationId)
                .projectId("project1")
                .invitedUserId(invitedUserId)
                .inviterUserId("user1")
                .status(InvitationStatus.ACCEPTED)
                .respondedAt(LocalDateTime.now())
                .build();

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> projectInvitationService.acceptInvitation(invitationId, invitedUserId));
        assertEquals("Приглашение уже обработано", exception.getMessage());

        verify(invitationRepository, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void declineInvitation_WhenInvitationNotFound_ShouldThrowException() {
        Long invitationId = 1L;
        String invitedUserId = "user2";

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> projectInvitationService.declineInvitation(invitationId, invitedUserId));
        assertEquals("Invitation not found", exception.getMessage());

        verify(invitationRepository, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void declineInvitation_WhenWrongUser_ShouldThrowException() {
        Long invitationId = 1L;
        String invitedUserId = "user2";
        String wrongUserId = "user3";

        ProjectInvitation invitation = ProjectInvitation.builder()
                .id(invitationId)
                .projectId("project1")
                .invitedUserId(invitedUserId)
                .inviterUserId("user1")
                .status(InvitationStatus.PENDING)
                .build();

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> projectInvitationService.declineInvitation(invitationId, wrongUserId));
        assertEquals("Это приглашение для другого пользователя", exception.getMessage());

        verify(invitationRepository, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void getPendingInvitations_ShouldReturnPendingInvitations() {
        String userId = "user1";
        List<ProjectInvitation> expectedInvitations = Arrays.asList(
                ProjectInvitation.builder().id(1L).build(),
                ProjectInvitation.builder().id(2L).build()
        );

        when(invitationRepository.findByInvitedUserIdAndStatus(userId, InvitationStatus.PENDING))
                .thenReturn(expectedInvitations);

        List<ProjectInvitation> result = projectInvitationService.getPendingInvitations(userId);

        assertEquals(expectedInvitations, result);
        verify(invitationRepository).findByInvitedUserIdAndStatus(userId, InvitationStatus.PENDING);
    }

    @Test
    void addProjectMember_ShouldCreateProjectMember() {
        Project project = new Project();
        project.setId("project1");

        User user = new User();
        user.setId("user1");

        projectInvitationService = new ProjectInvitationService(
                notificationRepository, invitationRepository, projectRepository,
                userRepository, projectMemberRepository, notificationService
        );
    }

    @Test
    void sendInvitationNotification_ShouldSendNotification() {
        Project project = new Project();
        project.setId("project1");
        project.setName("Test Project");

        User inviterUser = new User();
        inviterUser.setId("user1");
        inviterUser.setUsername("TestUser");

        ProjectInvitation invitation = ProjectInvitation.builder()
                .id(1L)
                .invitedUserId("user2")
                .build();
    }

    @Test
    void acceptInvitation_WhenProjectNotFound_ShouldThrowException() {
        Long invitationId = 1L;
        String invitedUserId = "user2";

        ProjectInvitation invitation = ProjectInvitation.builder()
                .id(invitationId)
                .projectId("non-existent-project")
                .invitedUserId(invitedUserId)
                .inviterUserId("user1")
                .status(InvitationStatus.PENDING)
                .build();

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(projectRepository.findById("non-existent-project")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> projectInvitationService.acceptInvitation(invitationId, invitedUserId));
        assertEquals("Project not found", exception.getMessage());
    }

    @Test
    void acceptInvitation_WhenUserNotFoundDuringProcessing_ShouldThrowException() {
        Long invitationId = 1L;
        String invitedUserId = "user2";

        ProjectInvitation invitation = ProjectInvitation.builder()
                .id(invitationId)
                .projectId("project1")
                .invitedUserId(invitedUserId)
                .inviterUserId("user1")
                .status(InvitationStatus.PENDING)
                .build();

        Project project = new Project();
        project.setId("project1");

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(projectRepository.findById("project1")).thenReturn(Optional.of(project));
        when(userRepository.findById(invitedUserId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> projectInvitationService.acceptInvitation(invitationId, invitedUserId));
        assertEquals("invited user not found", exception.getMessage());
    }
}