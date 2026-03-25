package io.github.dushyna.ticketflow.security.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequestDto(

        @Email
        @NotBlank
        String email
) {
}