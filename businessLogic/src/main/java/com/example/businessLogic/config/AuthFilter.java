package com.example.businessLogic.config;

import com.example.businessLogic.dto.SessionInfo;
import com.example.businessLogic.dto.UserInfo;
import com.example.businessLogic.service.AuthServiceClient;
import com.example.businessLogic.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;

@Component
@AllArgsConstructor
@Slf4j
public class AuthFilter extends OncePerRequestFilter {

    private final UserService userService;
    private final AuthServiceClient authServiceClient;
    private final SecurityContextRepository securityContextRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/actuator")){
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader("X-Auth-Token");

        if (token != null) {
            SessionInfo info = authServiceClient.getSessionInfo(token);
            if (info == null) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return;
            }

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    info.getUserId(),
                    null,
                    Collections.emptyList()
            );

            UserInfo userInfo = authServiceClient.getUserInfo(info.getUserId());
            userService.saveUser(userInfo);

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authToken);

            Duration duration = Duration.between(LocalDateTime.now(), info.getExpiresAt());
            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval((int) duration.toSeconds());

            securityContextRepository.saveContext(securityContext, request, response);
        } else {
            DeferredSecurityContext context = securityContextRepository.loadDeferredContext(request);
            Authentication auth = context.get().getAuthentication();

            if (auth == null || !auth.isAuthenticated()) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}