package com.example.businessLogic.service;

import com.example.businessLogic.dto.SessionInfo;
import com.example.businessLogic.dto.UserInfo;
import com.example.businessLogic.entity.User;
import com.example.businessLogic.exception.AuthServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@Slf4j
public class AuthServiceClient {
    private final WebClient authServiceWebClient;

    @Autowired
    public AuthServiceClient(WebClient authServiceClient) {
        this.authServiceWebClient = authServiceClient;
    }

    public SessionInfo getSessionInfo(String token) {
        try {
            return authServiceWebClient.post()
                    .uri("/get-session")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(token)
                    .retrieve()
                    .bodyToMono(SessionInfo.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return null;
            }
            throw new AuthServiceException("Не удалось выполнить запрос к сервису аутентификации", e);
        } catch (Exception e) {
            throw new AuthServiceException("Не удалось выполнить запрос к сервису аутентификации", e);
        }
    }

    public UserInfo getUserInfo(String userId) {
        try {
            return authServiceWebClient.post()
                    .uri("/get-user-info")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(userId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        if (clientResponse.statusCode() == HttpStatus.UNAUTHORIZED) {
                            log.debug("Неудачная проверка токена");
                            return Mono.empty();
                        }
                        return Mono.error(new RuntimeException("Ошибка: " + clientResponse.statusCode()));
                    })
                    .bodyToMono(UserInfo.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Ошибка сервиса авторизации для пользователя: {}, статус {}", userId, e.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("Неожиданная ошибка для пользователя: {}", userId, e);
            return null;
        }
    }

    public ResponseEntity<String> logout(String userId) {
        try {
            return authServiceWebClient.post()
                    .uri("/logout")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(userId)
                    .retrieve()
                    .toEntity(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (Exception e) {
            log.error("Ошибка при отправке запроса на AuthService для пользователя: {}", userId);
            return ResponseEntity.ok("{}");
        }
    }

    public ResponseEntity<Void> updateProfile(User user) {
        try {
            return authServiceWebClient.post()
                    .uri("/settings/update-profile")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(user)
                    .retrieve()
                    .toEntity(Void.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
