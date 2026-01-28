package com.example.businessLogic.service;

import com.example.businessLogic.dto.ProjectDto;
import com.example.businessLogic.dto.ProjectMemberDto;
import com.example.businessLogic.entity.Project;
import com.example.businessLogic.entity.ProjectMember;
import com.example.businessLogic.repository.ProjectMemberRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class ProjectMapper {

    private final ProjectMemberRepository projectMemberRepository;

    public ProjectDto toDto(Project project) {
        if (project == null) {
            return null;
        }

        List<ProjectMember> members = projectMemberRepository.findByProjectId(project.getId());

        return ProjectDto.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .createdAt(project.getCreatedAt())
                .members(members.stream()
                        .map(this::toMemberDto)
                        .toList())
                .build();
    }

    public List<ProjectDto> toDtoList(List<Project> projects) {
        return projects.stream()
                .map(this::toDto)
                .toList();
    }

    private ProjectMemberDto toMemberDto(ProjectMember projectMember) {
        return ProjectMemberDto.builder()
                .userId(projectMember.getUser().getId())
                .username(projectMember.getUser().getUsername())
                .role(projectMember.getRole().name())
                .build();
    }
}
