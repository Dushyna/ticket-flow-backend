package io.github.dushyna.ticketflow.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserCreateDto(
        @NotBlank
        @Pattern(
                regexp = "^(?=.{6,254}$)(?=.{1,64}@)[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@" +
                        "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?" +
                        "(?:\\.[A-Za-z]{2,})+$",
                message = "Invalid email format"
        )

        @Schema(
                description = "new User email",
                example = "tes_dev@upteams.de"
        )
        String email,

        @NotBlank
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9]).{8,}$",
                message = "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, and one number."
        )
        @Schema(
                description = "new User password",
                example = "dev_TR_pass_007"
        )
        String password) {
}
