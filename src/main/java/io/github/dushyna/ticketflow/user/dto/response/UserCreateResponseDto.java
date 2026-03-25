package io.github.dushyna.ticketflow.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;


public record UserCreateResponseDto(

        String id,

        @Schema(
                description = "User email",
                example = "homer@simpsons.com"
        )
        String email,

        @Schema(
                description = "Role granted to this User",
                example = "ROLE_USER",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String role,

        @Schema(
                description = "Flag indicating if the confirmation email was resent. True if the email was resent due to unconfirmed status; false if sent initially.",
                example = "true",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        boolean confirmationResent) {
}
