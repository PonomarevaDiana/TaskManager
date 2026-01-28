package com.example.businessLogic.service;

import com.example.businessLogic.dto.CreateProjectRequest;
import com.example.businessLogic.dto.ProjectMemberDto;
import com.example.businessLogic.entity.*;
import com.example.businessLogic.id.ProjectMemberId;
import com.example.businessLogic.repository.ProjectMemberRepository;
import com.example.businessLogic.repository.ProjectRepository;
import com.example.businessLogic.repository.TaskRepository;
import com.example.businessLogic.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final MetricsService metricsService;
    private final ProjectInvitationService projectInvitationService;

    public Project getProjectById(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Проект не найден: " + projectId));
    }

    @Transactional
    public Project createProject(CreateProjectRequest request, String ownerId) {
        User creator = userRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + ownerId));

        Project newProject = new Project();
        newProject.setName(request.getName());
        newProject.setDescription(request.getDescription());
        newProject.setCreatedAt(LocalDate.now());

        Project savedProject = projectRepository.save(newProject);

        addProjectMember(savedProject, creator, Role.ROLE_OWNER);

        for (String memberId : request.getMemberIds()) {
            if (!memberId.equals(ownerId)) {
                projectInvitationService.sendInvitation(savedProject.getId(), memberId, ownerId);
            }
        }

        metricsService.recordProjectCreated();

        return savedProject;
    }

    @Transactional
    public Project updateProject(String projectId, CreateProjectRequest request) {
        Project project = getProjectById(projectId);

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        updateProjectMembers(project, request.getMemberIds());

        return projectRepository.save(project);
    }

    @Transactional
    public Project updateAutoDeleteSettings(String projectId, Integer days, String userId) {
        Project project = getProjectById(projectId);

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AccessDeniedException("У вас нет доступа к этому проекту"));

        if (!member.getRole().equals(Role.ROLE_OWNER)) {
            throw new AccessDeniedException("У вас недостаточно прав, чтобы изменить настройки");
        }

        if (days != null && days < 0) {
            throw new IllegalArgumentException("Количество дней должно быть положительным или null");
        }

        project.setAutoDeleteDays(days);
        return projectRepository.save(project);
    }

    @Transactional
    public void deleteProject(String projectId) {
        Project project = getProjectById(projectId);

        taskRepository.deleteByProjectId(projectId);

        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
        if (!members.isEmpty()) {
            projectMemberRepository.deleteAll(members);
        }
        projectRepository.delete(project);
    }

    public List<Project> getUserProjects(String userId) {
        return projectRepository.findAllByUserId(userId);
    }

    public List<ProjectMemberDto> getProjectMembers(String projectId) {
        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);

        return members.stream()
                .map(member -> ProjectMemberDto.builder()
                        .userId(member.getUser().getId())
                        .username(member.getUser().getUsername())
                        .role(member.getRole().name())
                        .build()
                )
                .collect(Collectors.toList());
    }

    public void addProjectMember(Project project, User user, Role role) {

        ProjectMemberId projectMemberId = new ProjectMemberId();
        projectMemberId.setProject(project.getId());
        projectMemberId.setUser(user.getId());


        ProjectMember projectMember = new ProjectMember();
        projectMember.setId(projectMemberId);
        projectMember.setUser(user);
        projectMember.setProject(project);
        projectMember.setRole(role);

        projectMemberRepository.save(projectMember);
    }

    private void updateProjectMembers(Project project, List<String> memberIds) {
        Set<String> newMemberSet = new HashSet<>(memberIds);

        List<ProjectMember> currentMembers = projectMemberRepository.findByProjectId(project.getId());

        ProjectMember owner = currentMembers.stream()
                .filter(m -> m.getRole() == Role.ROLE_OWNER)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Владелец не найден"));

        if (!newMemberSet.contains(owner.getUser().getId())) {
            throw new IllegalArgumentException("Владельца нельзя удалить из проекта");
        }

        for (ProjectMember currentMember : currentMembers) {
            if (currentMember.getRole() != Role.ROLE_OWNER &&
                    !newMemberSet.contains(currentMember.getUser().getId())) {
                projectMemberRepository.delete(currentMember);
            }
        }

        Set<String> existingMemberIds = currentMembers.stream()
                .map(member -> member.getUser().getId())
                .collect(Collectors.toSet());

        for (String memberId : memberIds) {
            if (!existingMemberIds.contains(memberId)) {
                projectInvitationService.sendInvitation(project.getId(), memberId, owner.getUser().getId());
            }
        }
    }

}