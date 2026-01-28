package com.example.businessLogic.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectSettingsDto {
    private Integer autoDeleteDays;
}
