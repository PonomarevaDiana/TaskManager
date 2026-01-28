package com.example.businessLogic.controller;

import com.example.businessLogic.service.AuthServiceClient;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/logout")
public class LogoutController {

    private final AuthServiceClient authServiceClient;
    private final HttpSessionSecurityContextRepository securityContextRepository;

    public LogoutController(AuthServiceClient authServiceClient, HttpSessionSecurityContextRepository securityContextRepository) {
        this.authServiceClient = authServiceClient;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response, Authentication auth) {
        String userId = null;
        try {
            if (auth != null && auth.isAuthenticated()) {
                userId = auth.getPrincipal().toString();
            }

            if (userId != null) {
                ResponseEntity<String> authResponse = authServiceClient.logout(userId);
                List<String> setCookieHeaders = authResponse.getHeaders().get("Set-Cookie");
                if (setCookieHeaders != null) {
                    for (String setCookie : setCookieHeaders) {
                        response.addHeader("Set-Cookie", setCookie);
                    }
                }
            }

            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();

            SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
            securityContextRepository.saveContext(emptyContext, request, response);

            Cookie sessionCookie = new Cookie("JSESSIONID", "");
            sessionCookie.setMaxAge(0);
            sessionCookie.setPath("/");
            sessionCookie.setHttpOnly(true);
            sessionCookie.setSecure(request.isSecure());
            response.addCookie(sessionCookie);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();

            return ResponseEntity.ok().build();
        }
    }
}