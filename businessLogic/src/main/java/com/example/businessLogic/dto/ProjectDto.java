package com.example.businessLogic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProjectDto {
    private String id;
    private String name;
    private String description;
    private LocalDate createdAt;
    private List<ProjectMemberDto> members;
}