package com.example.businessLogic.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProjectMemberDto {
    private String userId;
    private String username;
    private String role;
}
