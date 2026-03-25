package io.github.dushyna.ticketflow.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;


@Schema(name = "NewUser", description = "Registration details")
public record LoginRequest(
        @NotBlank
        @Schema(description = "User's email address", example = "tes_dev@upteams.de")
        String email,

        @Schema(description = "User's password", example = "dev_TR_pass_007")
        @NotBlank
        String password) implements Serializable {
}
