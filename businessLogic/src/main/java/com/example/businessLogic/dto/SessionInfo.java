package com.example.businessLogic.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SessionInfo {
    private String sessionId;
    private String userId;
    private LocalDateTime expiresAt;
}
