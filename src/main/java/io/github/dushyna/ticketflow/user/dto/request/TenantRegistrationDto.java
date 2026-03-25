package io.github.dushyna.ticketflow.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Composite DTO for simultaneous registration of an Admin and their Organization")
public record TenantRegistrationDto(

        @Valid
        @NotNull
        @Schema(description = "Details for the administrative user")
        UserCreateDto admin,

        @Valid
        @NotNull
        @Schema(description = "Details for the new organization (cinema/theatre)")
        OrganizationCreateDto organization
) {
}
