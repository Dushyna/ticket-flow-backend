package io.github.dushyna.ticketflow.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request DTO for creating a new Organization (Cinema/Tenant)")
public record OrganizationCreateDto(

        @NotBlank
        @Schema(description = "Official name of the organization", example = "Cinema Star Central")
        String name,

        @NotBlank
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
        @Schema(description = "Unique URL-friendly identifier", example = "cinema-star-central")
        String slug,

        @Email
        @Schema(description = "Contact email address for the organization", example = "contact@cinemastar.com")
        String contactEmail
) {}
