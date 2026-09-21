package com.github.yildizmy.exception;

public class OpenFinanceUnavailableException extends RuntimeException {
    public OpenFinanceUnavailableException(String message) {
        super(message);
    }

    public OpenFinanceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
