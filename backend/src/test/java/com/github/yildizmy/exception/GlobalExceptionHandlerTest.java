package com.github.yildizmy.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    @Test
    void eachRuleCodeGetsTheStatusThatDescribesIt() {
        assertEquals(HttpStatus.NOT_FOUND, GlobalExceptionHandler.statusOf("GOAL_NOT_FOUND"));
        assertEquals(HttpStatus.NOT_FOUND, GlobalExceptionHandler.statusOf("RECURRENCE_NOT_FOUND"));
        assertEquals(HttpStatus.CONFLICT, GlobalExceptionHandler.statusOf("ENVELOPE_LIMIT"));
        assertEquals(HttpStatus.CONFLICT, GlobalExceptionHandler.statusOf("OPEN_FINANCE_READ_ONLY"));
        assertEquals(HttpStatus.CONFLICT, GlobalExceptionHandler.statusOf("IDEMPOTENCY_CONFLICT"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, GlobalExceptionHandler.statusOf("PUSH_UNAVAILABLE"));
        assertEquals(HttpStatus.BAD_REQUEST, GlobalExceptionHandler.statusOf("INVALID_AMOUNT"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, GlobalExceptionHandler.statusOf("SAME_WALLET"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, GlobalExceptionHandler.statusOf("SCENARIO_NO_CONFIRMED_INCOME"));
    }
}
