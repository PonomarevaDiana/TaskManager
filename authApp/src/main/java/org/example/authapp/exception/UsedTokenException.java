package org.example.authapp.exception;

public class UsedTokenException extends RuntimeException {
    public UsedTokenException(String message) {
        super(message);
    }
}
