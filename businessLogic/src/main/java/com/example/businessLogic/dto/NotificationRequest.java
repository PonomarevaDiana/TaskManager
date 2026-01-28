package com.example.businessLogic.dto;

import com.example.businessLogic.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationRequest {
    private String title;
    private String message;
    private String senderId;
    private NotificationType type;
    private Long invitationId;
}
