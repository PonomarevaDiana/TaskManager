package org.example.authapp.controller;

import lombok.RequiredArgsConstructor;
import org.example.authapp.entity.User;
import org.example.authapp.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private static final String SUCCESS = "success";

    private final UserService userService;

    @PostMapping("/update-profile")
    public ResponseEntity<Void> updateProfile(@RequestBody User form) {
        User user = userService.loadUserById(form.getId());

        boolean updated = userService.updateUser(user);

        if (updated) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/set-new-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody Map<String, Object> requestData) {
        try {
            String userId = requestData.get("userId").toString();
            String oldPassword = requestData.get("oldPassword").toString();
            String newPassword = requestData.get("newPassword").toString();

            User user = userService.loadUserById(userId);

            boolean changed = userService.changePassword(user, oldPassword, newPassword);

            if (!changed) {
                return ResponseEntity.badRequest().body(Map.of(
                        SUCCESS, false,
                        "error", "Текущий пароль неверен"
                ));
            }

            return ResponseEntity.ok(Map.of(
                    SUCCESS, true,
                    "message", "Пароль успешно изменен"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    SUCCESS, false,
                    "error", e.getMessage()
            ));
        }
    }
}