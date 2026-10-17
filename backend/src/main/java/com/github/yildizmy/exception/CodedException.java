package com.github.yildizmy.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CodedException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final long retryAfterSeconds;

    public CodedException(String code, HttpStatus status) {
        this(code, status, 0);
    }

    public CodedException(String code, HttpStatus status, long retryAfterSeconds) {
        super(code);
        this.code = code;
        this.status = status;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
