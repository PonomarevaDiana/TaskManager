package org.example.authapp.service;

import jakarta.mail.MessagingException;
import jakarta.security.auth.message.AuthException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.authapp.dto.AuthRequest;
import org.example.authapp.dto.AuthResponse;
import org.example.authapp.dto.SignupRequest;
import org.example.authapp.entity.Session;
import org.example.authapp.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;

    public AuthResponse login(@NonNull AuthRequest authRequest) throws AuthException {
        final User user = userService.loadUserByUsername(authRequest.getLogin());
        if (user == null || !user.isEnabled()) {
            throw new AuthException("Invalid username or password");
        }

        if (passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
            Session newSession = sessionService.createSession(user);
            return new AuthResponse(newSession.getAuthToken(), newSession.getRefreshToken());
        } else {
            throw new AuthException("Invalid password");
        }
    }

    public void signup(@NonNull SignupRequest signupRequest) throws AuthException, MessagingException, UnsupportedEncodingException {
        User userForm = new User(signupRequest.getUsername(),
                                    signupRequest.getPassword(),
                                    signupRequest.getEmail(),
                                    signupRequest.getFirstName(),
                                    signupRequest.getLastName());
        if (!userService.saveUser(userForm)) {
            throw new AuthException("User already exists");
        }
    }
}