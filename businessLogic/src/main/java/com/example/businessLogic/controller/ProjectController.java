package com.example.businessLogic.controller;

import com.example.businessLogic.dto.CreateProjectRequest;
import com.example.businessLogic.dto.ProjectDto;
import com.example.businessLogic.dto.ProjectMemberDto;
import com.example.businessLogic.dto.ProjectSettingsDto;
import com.example.businessLogic.entity.Project;
import com.example.businessLogic.entity.Task;
import com.example.businessLogic.service.ProjectMapper;
import com.example.businessLogic.service.ProjectService;
import com.example.businessLogic.service.TaskService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectService projectService;
    private final TaskService taskService;
    private final ProjectMapper projectMapper;

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getAllProjects() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        List<Project> projects = projectService.getUserProjects(userId);
        List<ProjectDto> projectDtos = projectMapper.toDtoList(projects);

        return ResponseEntity.ok(projectDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> getProject(@PathVariable String id) {
        Project project = projectService.getProjectById(id);
        ProjectDto projectDto = projectMapper.toDto(project);

        return ResponseEntity.ok(projectDto);
    }

    @PostMapping
    public ResponseEntity<ProjectDto> createProject(@Valid @RequestBody CreateProjectRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        Project project = projectService.createProject(request, userId);
        ProjectDto projectDto = projectMapper.toDto(project);

        return ResponseEntity.status(201).body(projectDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectDto> updateProject(@PathVariable String id, @Valid @RequestBody CreateProjectRequest request) {
        Project project = projectService.updateProject(id, request);
        ProjectDto projectDto = projectMapper.toDto(project);

        return ResponseEntity.ok(projectDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable String id) {
        projectService.deleteProject(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<Task>> getProjectTasks(@PathVariable String id) {
        List<Task> tasks = taskService.getProjectTasks(id);

        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<ProjectMemberDto>> getProjectMembers(@PathVariable String projectId) {
        List<ProjectMemberDto> members = projectService.getProjectMembers(projectId);
        return ResponseEntity.ok(members);
    }

    @PatchMapping("/{projectId}/settings/auto-delete")
    public ResponseEntity<Project> updateAutoDeleteSettings(@PathVariable String projectId,
                                                            @RequestParam(required = false) Integer days,
                                                            Authentication auth) {

        String userId = auth.getPrincipal().toString();
        Project project = projectService.updateAutoDeleteSettings(projectId, days, userId);

        return ResponseEntity.ok(project);
    }

    @GetMapping("/{projectId}/settings")
    public ResponseEntity<ProjectSettingsDto> getProjectSettings(@PathVariable String projectId, Authentication auth) {
        Project project = projectService.getProjectById(projectId);
        ProjectSettingsDto settings = ProjectSettingsDto.builder()
                .autoDeleteDays(project.getAutoDeleteDays())
                .build();

        return ResponseEntity.ok(settings);
    }
}
