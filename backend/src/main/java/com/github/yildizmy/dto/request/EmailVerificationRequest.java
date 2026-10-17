package com.github.yildizmy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** {@code login} is the username or the e-mail, as on the login form. */
public record EmailVerificationRequest(
        @NotBlank @Size(max = 50) String login,
        @NotBlank @Pattern(regexp = "\\d{8}") String code) {
}
