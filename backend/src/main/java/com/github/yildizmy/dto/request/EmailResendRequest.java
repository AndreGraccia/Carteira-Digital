package com.github.yildizmy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailResendRequest(@NotBlank @Size(max = 50) String login) {
}
