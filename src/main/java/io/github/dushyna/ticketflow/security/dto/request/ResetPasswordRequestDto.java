package io.github.dushyna.ticketflow.security.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDto(

        @NotBlank
        String token,

        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword
) {
}