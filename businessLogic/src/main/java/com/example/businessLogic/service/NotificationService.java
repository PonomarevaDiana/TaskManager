package com.example.businessLogic.service;

import com.example.businessLogic.dto.NotificationRequest;
import com.example.businessLogic.dto.NotificationResponse;
import com.example.businessLogic.entity.Notification;
import com.example.businessLogic.entity.NotificationType;
import com.example.businessLogic.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;

    public List<NotificationResponse> getUserNotifications(String ownerId) {
        if (ownerId == null || ownerId.isEmpty()) {
            throw new NullPointerException("OwnerId не может быть пустым");
        }

        List<Notification> notifications = notificationRepository.findByOwnerIdOrderByDateDesc(ownerId);

        return notifications.stream()
                .map(this::convertToResponse)
                .toList();
    }

    public List<NotificationResponse> getUnreadNotifications(String ownerId) {
        if (ownerId == null || ownerId.isEmpty()) {
            throw new NullPointerException("OwnerId не может быть пустым");
        }

        List<Notification> notifications = notificationRepository.findByOwnerIdAndIsReadFalseOrderByDateDesc(ownerId);

        return notifications.stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String ownerId) {
        if (ownerId == null || ownerId.isEmpty()) {
            throw new NullPointerException("OwnerId не может быть пустым");
        }

        return notificationRepository.countByOwnerIdAndIsReadFalse(ownerId);
    }

    public void sendNotification(String ownerId, NotificationRequest request) {
        if (ownerId == null || ownerId.isEmpty()) {
            throw new NullPointerException("OwnerId не может быть пустым");
        }

        Notification notification = Notification.builder()
                .ownerId(ownerId)
                .title(request.getTitle())
                .message(request.getMessage())
                .senderId(request.getSenderId())
                .type(request.getType())
                .isRead(false)
                .invitationId(request.getInvitationId())
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Уведомление создано: id={}, ownerId={}, type={}", saved.getId(), saved.getOwnerId(), saved.getType());

        Map<String, Object> wsNotification = convertToWebSocketMessage(saved);
        notificationSender.sendNotificationToUser(ownerId, wsNotification);
    }

    @Transactional
    public void markAsRead(Long notificationId, String ownerId) {
        if (ownerId == null || ownerId.isEmpty() || notificationId == null) {
            throw new NullPointerException("ownerId и notificationId не могут быть null");
        }

        notificationRepository.markAsRead(notificationId, ownerId);
        log.info("Уведомление {} отмечено как прочитанное пользователем {}", notificationId, ownerId);

        Map<String, Object> message = new HashMap<>();
        message.put("type", NotificationType.NOTIFICATION_READ);
        message.put("notificationId", notificationId);
        message.put("isRead", true);
        notificationSender.sendMessage(ownerId, message);
    }

    @Transactional
    public void markAllAsRead(String ownerId) {
        if (ownerId == null || ownerId.isEmpty()) {
            throw new NullPointerException("ownerId не может быть null");
        }

        notificationRepository.markAllAsRead(ownerId);
        log.info("Все уведомления отмечены как прочитанные пользователем {}", ownerId);

        List<NotificationResponse> notifications = getUserNotifications(ownerId);
        Map<String, Object> message = new HashMap<>();
        message.put("type", NotificationType.NOTIFICATIONS_LIST);
        message.put("notifications", notifications);
        notificationSender.sendMessage(ownerId, message);
    }

    @Transactional
    public void deleteNotification(Long notificationId, String ownerId) {
        if (ownerId == null || ownerId.isEmpty() || notificationId == null) {
            throw new NullPointerException("ownerId и notificationId не могут быть null");
        }

        notificationRepository.deleteByOwnerIdAndId(ownerId, notificationId);
        log.info("Уведомление {} удалено пользователем {}", notificationId, ownerId);

        Map<String, Object> message = new HashMap<>();
        message.put("type", NotificationType.NOTIFICATION_DELETED);
        message.put("notificationId", notificationId);
        notificationSender.sendMessage(ownerId, message);
    }

    @Transactional
    public void deleteAllNotifications(String ownerId) {
        if (ownerId == null || ownerId.isEmpty()) {
            throw new NullPointerException("ownerId не может быть null");
        }

        notificationRepository.deleteAllByOwnerId(ownerId);
        log.info("Все уведомления удалены для пользователя {}", ownerId);

        Map<String, Object> message = new HashMap<>();
        message.put("type", NotificationType.NOTIFICATIONS_LIST);
        message.put("notifications", List.of());
        notificationSender.sendMessage(ownerId, message);
    }

    @Transactional
    public void cleanupOldNotifications(String ownerId) {
        if (ownerId == null || ownerId.isEmpty()) {
            throw new NullPointerException("ownerId не может быть null");
        }

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        notificationRepository.deleteOldNotifications(ownerId, thirtyDaysAgo);
        log.info("Старые уведомления очищены для пользователя {}", ownerId);

        List<NotificationResponse> notifications = getUserNotifications(ownerId);

        Map<String, Object> message = new HashMap<>();
        message.put("type", NotificationType.NOTIFICATIONS_LIST);
        message.put("notifications", notifications);
        notificationSender.sendMessage(ownerId, message);
    }

    private NotificationResponse convertToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .senderId(notification.getSenderId())
                .type(notification.getType())
                .isRead(notification.getIsRead())
                .date(notification.getDate())
                .invitationId(notification.getInvitationId())
                .build();
    }

    private Map<String, Object> convertToWebSocketMessage(Notification notification) {
        Map<String, Object> message = new HashMap<>();
        message.put("id", notification.getId());
        message.put("title", notification.getTitle());
        message.put("message", notification.getMessage());
        message.put("senderId", notification.getSenderId());
        message.put("type", notification.getType());
        message.put("isRead", notification.getIsRead());
        message.put("date", notification.getDate());
        message.put("invitationId", notification.getInvitationId());
        return message;
    }
}