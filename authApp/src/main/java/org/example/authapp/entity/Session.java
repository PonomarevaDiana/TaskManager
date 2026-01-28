package org.example.authapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "sessions")
@Table(name="sessions", indexes = {
        @Index(name = "idx_authorization_sessions_auth_token", columnList = "auth_token"),
        @Index(name = "idx_authorization_sessions_refresh_token", columnList = "refresh_token")
})
public class Session implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String sessionId;
    private String userId;
    private String authToken;
    private boolean authTokenExpired;
    private String refreshToken;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt = LocalDateTime.now();
}
