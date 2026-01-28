package org.example.authapp.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class MetricsService {

    private static final String AUTH_SERVICE = "auth-service";
    private static final String APPLICATION = "application";

    private final Counter loginSuccessCounter;

    private final Counter loginFailureCounter;

    private final Counter registryCounter;

    private final Timer requestDurationTimer;

    public MetricsService(MeterRegistry meterRegistry) {
        this.loginSuccessCounter = Counter.builder("auth.login.success")
                .description("Количество успешных логинов")
                .tag(APPLICATION, AUTH_SERVICE)
                .register(meterRegistry);

        this.loginFailureCounter = Counter.builder("auth.login.failure")
                .description("Количество неудачных попыток логина")
                .tag(APPLICATION, AUTH_SERVICE)
                .register(meterRegistry);

        this.registryCounter = Counter.builder("auth.registry.success")
                .description("Количество успешных регистраций")
                .tag(APPLICATION, AUTH_SERVICE)
                .register(meterRegistry);

        this.requestDurationTimer = Timer.builder("http.request.duration")
                .description("Длительность обработки http-запроса")
                .publishPercentiles(0.5, 0.95, 0.99)
                .tag(APPLICATION, AUTH_SERVICE)
                .register(meterRegistry);
    }

    public void recordLoginSuccess() {
        loginSuccessCounter.increment();
    }

    public void recordLoginFailure() {
        loginFailureCounter.increment();
    }

    public void recordRegistrySuccess() {
        registryCounter.increment();
    }

    public void recordRegistryFailure() {
        registryCounter.increment();
    }

    public void recordRequestDuration(long durationMs) {
        requestDurationTimer.record(Duration.ofMillis(durationMs));
    }
}