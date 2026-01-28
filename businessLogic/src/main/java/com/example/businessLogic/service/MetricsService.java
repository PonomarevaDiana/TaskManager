package com.example.businessLogic.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;

    private final Counter taskCreatedCounter;

    private final Counter projectCreatedCounter;

    private final Timer requestDurationTimer;

    private final Timer sessionDurationTimer;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.taskCreatedCounter = Counter.builder("task.сreated")
                .description("Количество созданных задач")
                .tag("task", "created")
                .register(this.meterRegistry);

        this.projectCreatedCounter = Counter.builder("project.created")
                .description("Количество созданных проектов")
                .tag("project", "created")
                .register(this.meterRegistry);

        this.requestDurationTimer = Timer.builder("http.request.duration")
                .description("Длительность обработки http запроса")
                .publishPercentiles(0.5, 0.95, 0.99)
                .tag("application", "business-logic")
                .register(this.meterRegistry);

        this.sessionDurationTimer = Timer.builder("session.duration")
                .description("Длительность сессии")
                .publishPercentiles(0.5, 0.95, 0.99)
                .tag("application", "business-logic")
                .register(this.meterRegistry);
    }

    public void recordTaskCreated() {
        this.taskCreatedCounter.increment();
    }

    public void recordProjectCreated() {
        this.projectCreatedCounter.increment();
    }

    public void recordRequestDuration(long durationMs) {
        requestDurationTimer.record(java.time.Duration.ofMillis(durationMs));
    }

    public void recordSessionDuration(long durationMs) {
        sessionDurationTimer.record(java.time.Duration.ofMillis(durationMs));
    }
}