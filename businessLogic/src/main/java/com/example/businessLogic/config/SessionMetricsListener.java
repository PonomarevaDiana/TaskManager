package com.example.businessLogic.config;

import com.example.businessLogic.service.MetricsService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class SessionMetricsListener implements HttpSessionListener {
    private final MetricsService metricsService;

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        session.setAttribute("sessionStart", System.currentTimeMillis());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        Long startTime = (Long) session.getAttribute("sessionStart");
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordSessionDuration(duration);
        }
    }
}