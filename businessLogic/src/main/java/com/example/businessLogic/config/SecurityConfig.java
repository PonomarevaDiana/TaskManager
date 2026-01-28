package com.example.businessLogic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthFilter authFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/logout").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                ((request, response, authException) -> response.sendError(HttpStatus.UNAUTHORIZED.value())))
                        .accessDeniedHandler(
                                ((request, response, accessDeniedException) -> response.sendError(HttpStatus.FORBIDDEN.value()))));

        return http.build();
    }

    @Bean
    public HttpSessionSecurityContextRepository securityContextRepository() {
        HttpSessionSecurityContextRepository repo = new HttpSessionSecurityContextRepository();
        repo.setAllowSessionCreation(true);
        return repo;
    }
}