package org.example.authapp.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authapp.dto.SessionInfo;
import org.example.authapp.entity.Session;
import org.example.authapp.entity.User;
import org.example.authapp.exception.InvalidTokenException;
import org.example.authapp.exception.SessionExpiredException;
import org.example.authapp.exception.UsedTokenException;
import org.example.authapp.repository.SessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class SessionService {
    private SessionRepository sessionRepository;

    public Session findByAuthToken(String token) {
        Session session = sessionRepository.findByAuthToken(token);

        if (session == null) {
            log.warn("INVALID_TOKEN_ATTEMPT - Token not found in database");
            throw new InvalidTokenException("Invalid token");
        } else if (session.isAuthTokenExpired()) {
            log.warn("USED_TOKEN_ATTEMPT - Session ID: {}, User ID: {}",
                    session.getSessionId(), session.getUserId());
            throw new UsedTokenException("Token has been used");
        } else if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("EXPIRED_SESSION_ATTEMPT - Session ID: {}, User ID: {}, Expired at: {}",
                    session.getSessionId(), session.getUserId(), session.getExpiresAt());
            throw new SessionExpiredException("Expired session");
        }
        log.info("TOKEN_VALIDATED_SUCCESS - Session ID: {}, User ID: {}",
                session.getSessionId(), session.getUserId());
        return session;
    }

    public Session findByRefreshToken(String token) {
        Session session = sessionRepository.findByRefreshToken(token);

        if (session == null) {
            log.warn("INVALID_REFRESH_TOKEN - Refresh token not found");
            throw new InvalidTokenException("Invalid token");
        } else if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("EXPIRED_REFRESH_SESSION - Session ID: {}, User ID: {}, Expired at: {}",
                    session.getSessionId(), session.getUserId(), session.getExpiresAt());
            throw new SessionExpiredException("Expired session");
        }
        log.info("REFRESH_TOKEN_VALIDATED - Session ID: {}, User ID: {}",
                session.getSessionId(), session.getUserId());
        return session;
    }

    public Session createSession(User user) {
        final String authToken = TokenUtils.generateToken();
        final String refreshToken = TokenUtils.generateToken();

        Session session = new Session();
        session.setUserId(user.getId());
        session.setAuthToken(authToken);
        session.setRefreshToken(refreshToken);
        session.setExpiresAt(LocalDateTime.now().plusHours(24));
        sessionRepository.save(session);
        log.info("SESSION_CREATED - User ID: {}, Username: {}, Session ID: {}, Expires at: {}",
                user.getId(), user.getUsername(), session.getSessionId(), session.getExpiresAt());

        return session;
    }

    public Session getSessionByAuthToken(String token) {
        Session session = findByAuthToken(token);
        session.setAuthTokenExpired(true);
        sessionRepository.save(session);
        log.info("AUTH_TOKEN_USED - Session ID: {}, User ID: {}, Marked as used",
                session.getSessionId(), session.getUserId());

        return session;
    }

    public Session getSessionByRefreshToken(String token) {
        Session session = findByRefreshToken(token);
        session.setAuthToken(TokenUtils.generateToken());
        session.setAuthTokenExpired(false);
        sessionRepository.save(session);
        log.info("TOKEN_REFRESHED - Session ID: {}, User ID: {}, New token generated",
                session.getSessionId(), session.getUserId());

        return session;
    }

    public SessionInfo getInfoFromSession(Session session) {
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setUserId(session.getUserId());
        sessionInfo.setSessionId(session.getSessionId());
        sessionInfo.setExpiresAt(session.getExpiresAt());

        log.info("SESSION_INFO_REQUESTED - Session ID: {}, User ID: {}",
                session.getSessionId(), session.getUserId());
        return sessionInfo;
    }

    @Transactional
    public void deleteAllByUserId(String userId) {
        sessionRepository.deleteAllByUserId(userId);
    }

    public List<Session> findSessionsByUserId(String userId) {
        return sessionRepository.findByUserId(userId);
    }


    public void logLogoutStatistics(String userId) {
        List<Session> sessions = sessionRepository.findByUserId(userId);
        LocalDateTime now = LocalDateTime.now();

        if (sessions.isEmpty()) {
            log.info("LOGOUT_STATS - User ID: {}, No active sessions", userId);
            return;
        }

        long totalDurationMinutes = 0;
        int expiredSessions = 0;
        int activeSessions = 0;

        for (Session session : sessions) {
            if (session.getCreatedAt() != null) {
                long sessionMinutes = java.time.Duration.between(
                        session.getCreatedAt(), now
                ).toMinutes();
                totalDurationMinutes += sessionMinutes;

                if (session.getExpiresAt().isBefore(now)) {
                    expiredSessions++;
                } else {
                    activeSessions++;
                }

                long hours = sessionMinutes / 60;
                long minutes = sessionMinutes % 60;

                log.info("SESSION_LOGOUT - ID: {}, User: {}, "
                                + "Duration: {}ч {}м, Created: {}, Status: {}",
                        session.getSessionId(),
                        session.getUserId(),
                        hours, minutes,
                        session.getCreatedAt(),
                        session.getExpiresAt().isBefore(now) ? "EXPIRED" : "ACTIVE");
            }
        }

        long avgDurationMinutes = sessions.size() > 0 ? totalDurationMinutes / sessions.size() : 0;
        long avgHours = avgDurationMinutes / 60;
        long avgMinutes = avgDurationMinutes % 60;


        log.info("LOGOUT_SUMMARY - User ID: {}, "
                        + "Total sessions: {}, Active: {}, Expired: {}, "
                        + "Avg duration: {}ч {}м, Timestamp: {}",
                userId,
                sessions.size(),
                activeSessions,
                expiredSessions,
                avgHours, avgMinutes,
                now);
    }
}