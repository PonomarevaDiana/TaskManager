package com.example.businessLogic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSender {

    private final ObjectMapper objectMapper;

    private static final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    public void registerSession(String userId, WebSocketSession session) {
        userSessions.put(userId, session);
    }

    public void removeSession(String userId) {
        userSessions.remove(userId);
    }

    public void sendNotificationToUser(String userId, Map<String, Object> notification) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "NEW_NOTIFICATION");
            message.put("notification", notification);
            sendMessage(userId, message);
        } catch (Exception e) {
            log.error("Ошибка отправки уведомления пользователю: ", e);
        }
    }

    public void sendMessage(String userId, Map<String, Object> message) {
        try {
            WebSocketSession session = userSessions.get(userId);
            if (session != null && session.isOpen()) {
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
            }
        } catch (Exception e) {
            log.error("Ошибка отправки сообщения WebSocket", e);
        }
    }

    public String findUserIdBySession(WebSocketSession session) {
        return userSessions.entrySet().stream()
                .filter(e -> e.getValue().equals(session))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
