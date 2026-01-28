package com.example.businessLogic.controller;

import com.example.businessLogic.dto.UserInfo;
import com.example.businessLogic.entity.User;
import com.example.businessLogic.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private static final String SUCCESS = "success";
    private static final String MESSAGE = "message";

    private final UserService userService;

    @PostMapping("/update-profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody Map<String, String> userData, Authentication authentication) {
        User user = userService.findByUserId(authentication.getPrincipal().toString());

        UserInfo form = new UserInfo();
        form.setUsername(userData.get("username"));
        form.setFirstName(userData.get("firstName"));
        form.setLastName(userData.get("lastName"));

        boolean updated = userService.updateUser(user, form);

        if (updated) {
            return ResponseEntity.ok(Map.of(
                    SUCCESS, true,
                    MESSAGE, "Профиль успешно обновлен"
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    SUCCESS, false,
                    MESSAGE, "Ошибка обновления профиля"
            ));
        }
    }
}
