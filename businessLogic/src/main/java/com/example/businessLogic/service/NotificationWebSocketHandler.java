package com.example.businessLogic.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final NotificationSender notificationSender;

    public NotificationWebSocketHandler(NotificationService service, NotificationSender sender) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.notificationService = service;
        this.notificationSender = sender;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String userId = session.getUri().getQuery().split("=")[1];

        if (userId != null) {
            notificationSender.registerSession(userId, session);

            Map<String, Object> notification = createMessage("NOTIFICATIONS_LIST",
                    notificationService.getUserNotifications(userId));

            notificationSender.sendMessage(userId, notification);
        } else {
            log.warn("userId не найден в URI для сессии: {}", session.getId());
            session.close();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            Map<String, Object> data = objectMapper.readValue(message.getPayload(), new TypeReference<>() {});

            String type = (String) data.get("type");
            String userId = (String) data.get("userId");

            if (userId == null || userId.isEmpty()) {
                log.warn("userId отсутствует в сообщении типа {}", type);
                return;
            }

            switch (type) {
                case "GET_NOTIFICATIONS":
                    notificationService.getUserNotifications(userId);
                    break;
                case "MARK_AS_READ":
                    notificationService.markAsRead(((Number) data.get("notificationId")).longValue(), userId);
                    break;
                case "MARK_ALL_AS_READ":
                    notificationService.markAllAsRead(userId);
                    break;
                case "DELETE_NOTIFICATION":
                    notificationService.deleteNotification(((Number) data.get("notificationId")).longValue(), userId);
                    break;
                case "DELETE_ALL_NOTIFICATIONS":
                    notificationService.deleteAllNotifications(userId);
                    break;
                case "PING":
                    notificationSender.sendMessage(userId, createMessage("PONG", null));
                    break;
                default:
                    log.warn("Неизвестный тип сообщения: {}", type);
            }
        } catch (Exception e) {
            log.error("Ошибка обработки сообщения WebSocket", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = notificationSender.findUserIdBySession(session);

        if (userId != null) {
            notificationSender.removeSession(userId);
            log.info("Пользователь отключился: userId={}", userId);
        }
    }

    private Map<String, Object> createMessage(String type, Object data) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        if (data != null) {
            if (type.equals("NEW_NOTIFICATION")) {
                message.put("notification", data);
            } else {
                message.put("data", data);
            }
        }

        return message;
    }
}