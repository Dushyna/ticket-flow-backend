package io.github.dushyna.ticketflow.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "DTO for updating user profile details")
public record UpdateUserDetailsDto(

        @Schema(example = "Homer")
        String firstName,

        @Schema(example = "Simpson")
        String lastName,

        @Schema(description = "User's birth date", example = "1990-05-15")
        LocalDate birthDate,

        @Schema(example = "+380501234567")
        String phone
) {
}
