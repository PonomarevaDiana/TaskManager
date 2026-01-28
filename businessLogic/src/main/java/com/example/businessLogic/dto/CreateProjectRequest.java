package com.example.businessLogic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateProjectRequest {
    @NotBlank
    private String name;

    private String description;

    @NotEmpty
    private List<String> memberIds;
}
