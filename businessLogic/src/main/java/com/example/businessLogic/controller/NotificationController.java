package com.example.businessLogic.controller;

import com.example.businessLogic.dto.NotificationRequest;
import com.example.businessLogic.dto.NotificationResponse;
import com.example.businessLogic.service.NotificationService;
import com.example.businessLogic.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String userId = userService.findByUserId(authentication.getPrincipal().toString()).getId();

        List<NotificationResponse> notifications = notificationService.getUserNotifications(userId);

        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String userId = userService.findByUserId(authentication.getPrincipal().toString()).getId();

        List<NotificationResponse> unreadNotifications = notificationService.getUnreadNotifications(userId);

        return ResponseEntity.ok(unreadNotifications);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String userId = userService.findByUserId(authentication.getPrincipal().toString()).getId();

        Long unreadCount = notificationService.getUnreadCount(userId);

        return ResponseEntity.ok(unreadCount);
    }

    @PostMapping
    public ResponseEntity<Void> createNotification(@RequestBody NotificationRequest request, Authentication authentication){
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String userId = userService.findByUserId(authentication.getPrincipal().toString()).getId();

        notificationService.sendNotification(userId, request);

        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable("id") Long notificationId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String userId = userService.findByUserId(authentication.getPrincipal().toString()).getId();

        notificationService.markAsRead(notificationId, userId);

        return ResponseEntity.status(202).build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String userId = userService.findByUserId(authentication.getPrincipal().toString()).getId();

        notificationService.markAllAsRead(userId);

        return ResponseEntity.status(202).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable("id") Long notificationId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String userId = userService.findByUserId(authentication.getPrincipal().toString()).getId();

        notificationService.deleteNotification(notificationId, userId);

        return ResponseEntity.status(204).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllNotifications(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String userId = userService.findByUserId(authentication.getPrincipal().toString()).getId();

        notificationService.deleteAllNotifications(userId);

        return ResponseEntity.status(204).build();
    }
}