package io.github.dushyna.ticketflow.user.dto.response;

import io.github.dushyna.ticketflow.user.entity.ConfirmationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Detailed user profile information")
public record UserResponseDto(
        @Schema(description = "Unique identifier (UUID)",
                example = "550e8400-e29b-41d4-a716-446655440000",
                accessMode = Schema.AccessMode.READ_ONLY)
        String id,

        @Schema(description = "User's email address",
                example = "homer@simpsons.com")
        String email,

        @Schema(description = "User's first name",
                example = "Homer")
        String firstName,

        @Schema(description = "User's last name",
                example = "Simpson")
        String lastName,

        @Schema(description = "User's date of birth",
                example = "1990-05-15")
        LocalDate birthDate,

        @Schema(description = "User's phone number",
                example = "+15550123456")
        String phone,

        @Schema(description = "Assigned user role",
                example = "ROLE_USER")
        String role,

        @Schema(description = "Account confirmation status",
                example = "CONFIRMED")
        ConfirmationStatus confirmationStatus,

        @Schema(description = "ID of the organization (cinema) the user belongs to",
                example = "7d7fb710-123e-412b-b570-76eb047d93c1",
                nullable = true)
        String organizationId
) {}
