package com.example.businessLogic.dto;

import com.example.businessLogic.entity.NotificationType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private String senderId;
    private NotificationType type;
    private Boolean isRead;
    private Long invitationId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime date;
}
