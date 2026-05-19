package io.github.dushyna.ticketflow.user.dto.request;

import io.github.dushyna.ticketflow.user.entity.Role;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UserRoleUpdateRequestDto(
        @NotNull(message = "User ID is required")
        UUID userId,

        @NotNull(message = "Role is required")
        Role role
) {}
