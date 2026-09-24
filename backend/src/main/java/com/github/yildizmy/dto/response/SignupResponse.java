package com.github.yildizmy.dto.response;

import java.time.Instant;

/**
 * The same answer for a new address and for one that already has an account, so the sign-up form never
 * reveals which e-mails are registered. The code (or the "you already have an account" notice) goes by e-mail.
 */
public record SignupResponse(String maskedEmail, Instant resendAvailableAt) {
}
