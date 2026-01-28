package org.example.authapp.service;

import java.util.UUID;

public class TokenUtils {

    private TokenUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String generateToken() {
        return UUID.randomUUID().toString();
    }

}