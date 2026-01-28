package org.example.authapp.controller;

import jakarta.mail.MessagingException;
import jakarta.security.auth.message.AuthException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authapp.dto.*;
import org.example.authapp.entity.Session;
import org.example.authapp.entity.User;
import org.example.authapp.service.AuthService;
import org.example.authapp.service.MetricsService;
import org.example.authapp.service.SessionService;
import org.example.authapp.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.time.Duration;

@RestController
@RequestMapping("/")
@AllArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final SessionService sessionService;
    private final UserService userService;
    private final MetricsService metricsService;

    @GetMapping("/pre-login")
    public ResponseEntity<String> preLogin(@CookieValue(value = "refreshToken", defaultValue = "") String refresh,
                                           HttpServletResponse response) {
        if (!refresh.isEmpty()) {
            try {
                final Session session = sessionService.getSessionByRefreshToken(refresh);
                if (session != null) {
                    log.info("AUTH_PRELOGIN_SUCCESS - UserId: {}", session.getUserId());
                    return ResponseEntity.ok(session.getAuthToken());
                }
            } catch (Exception e) {
                log.warn("AUTH_PRELOGIN_TOKEN_ERROR - Reason: {}", e.getMessage());
            }
        }

        Cookie refreshToken = createRefreshToken("", Duration.ZERO);
        response.addCookie(refreshToken);

        return ResponseEntity.status(401).build();
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AuthRequest authRequest,
                                        HttpServletResponse response) {


        final AuthResponse authResponse;
        try {
            authResponse = authService.login(authRequest);
            Cookie refreshToken = createRefreshToken(authResponse.getAuthToken(), Duration.ofHours(24));
            response.addCookie(refreshToken);
            log.info("AUTH_LOGIN_SUCCESS - UserId extracted from token");
            metricsService.recordLoginSuccess();


        } catch (AuthException e) {
            metricsService.recordLoginFailure();
            log.warn("AUTH_LOGIN_FAILED - Username: {}, Reason: {}",
                    authRequest.getLogin(), e.getMessage());
            return ResponseEntity.status(401).body(e.getMessage());
        }

        return ResponseEntity.ok(authResponse.getAuthToken());
    }

    @PostMapping("/get-session")
    public ResponseEntity<SessionInfo> getSession(@RequestBody String token) {

        try {
            final Session session = sessionService.getSessionByAuthToken(token);
            final SessionInfo sessionInfo = sessionService.getInfoFromSession(session);
            log.info("AUTH_SESSION_VALID - UserId: {}", session.getUserId());
            return ResponseEntity.ok(sessionInfo);
        }
        catch (Exception e) {
            log.warn("AUTH_SESSION_ERROR - Reason: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest signupRequest) {
        try {
            authService.signup(signupRequest);
            log.info("AUTH_SIGNUP_SUCCESS - Username: {}", signupRequest.getUsername());
            metricsService.recordRegistrySuccess();
        } catch (AuthException | MessagingException | UnsupportedEncodingException e) {
            log.warn("AUTH_SIGNUP_FAILED - Username: {}, Reason: {}",
                    signupRequest.getUsername(), e.getMessage());
            metricsService.recordRegistryFailure();
            return ResponseEntity.status(401).body(e.getMessage());
        }

        return ResponseEntity.ok("ok");
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam("code") String code,
                                         HttpServletResponse response) {

        User newUser = userService.verify(code);
        if (newUser != null) {
            Session newSession = sessionService.createSession(newUser);
            response.addCookie(createRefreshToken(newSession.getRefreshToken(), Duration.ofDays(30)));
            log.info("AUTH_EMAIL_VERIFIED - UserId: {}", newUser.getId());
            return ResponseEntity.ok(newSession.getAuthToken());
        } else {
            log.warn("AUTH_EMAIL_VERIFY_FAILED");
            return ResponseEntity.status(401).body("Неверный код или регистрация уже подтверждена");
        }
    }

    @PostMapping("/get-user-info")
    public ResponseEntity<UserInfo> getUserInfo(@RequestBody String userId) {

        final User user = userService.loadUserById(userId);
        if (user == null) {
            log.warn("AUTH_USER_INFO_NOT_FOUND - RequestedId: {}", userId);
            return ResponseEntity.status(404).build();
        }
        UserInfo userInfo = new UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoles()
        );
        log.info("AUTH_USER_INFO_RETRIEVED - UserId: {}", userId);

        return ResponseEntity.ok(userInfo);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response,
                                       @RequestBody String userId) {

        sessionService.logLogoutStatistics(userId);

        sessionService.deleteAllByUserId(userId);

        Cookie refreshToken = createRefreshToken("", Duration.ZERO);
        response.addCookie(refreshToken);
        log.info("LOGOUT_FINISHED - User ID: {}", userId);

        return ResponseEntity.ok().build();
    }

    private Cookie createRefreshToken(String value, Duration duration) {
        Cookie cookie = new Cookie("refreshToken", value);
        cookie.setMaxAge((int) duration.getSeconds());
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setDomain("localhost");

        return cookie;
    }
}