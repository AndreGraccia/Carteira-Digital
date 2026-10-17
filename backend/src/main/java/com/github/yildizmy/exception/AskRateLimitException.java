package com.github.yildizmy.exception;

public class AskRateLimitException extends RuntimeException {
    public AskRateLimitException() {
        super("ASK_RATE_LIMIT");
    }
}
