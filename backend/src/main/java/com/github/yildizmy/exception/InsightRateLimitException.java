package com.github.yildizmy.exception;

public class InsightRateLimitException extends RuntimeException {
    public InsightRateLimitException() {
        super("INSIGHT_RATE_LIMIT");
    }
}
