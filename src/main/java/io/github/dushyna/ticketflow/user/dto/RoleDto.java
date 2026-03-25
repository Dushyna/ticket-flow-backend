package io.github.dushyna.ticketflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Data Transfer Object for Role entity")
public record RoleDto(
        @Schema(
                description = "Title of the Role",
                example = "ROLE_ADMIN"
        )
        String name) {
}
