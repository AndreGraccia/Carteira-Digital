package com.github.yildizmy.exception;

public class AuthenticationRateLimitException extends RuntimeException {
    public AuthenticationRateLimitException() {
        super("AUTH_RATE_LIMIT");
    }
}
