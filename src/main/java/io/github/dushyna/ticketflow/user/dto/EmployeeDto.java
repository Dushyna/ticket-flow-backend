package io.github.dushyna.ticketflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Employee DTO
 *
 * @param id       Employee ID
 * @param firstName     Employee's first name
 * @param lastName     Employee's last name
 * @param email    Employee's email
 * @param role    Roles of the Employee for authorization process
 * @param  organizationId   id of  organization
 */
@Schema(description = "Data Transfer Object for Employee entity")
public record EmployeeDto(
        @Schema(
                description = "Unique identifier of the Employee",
                example = "9",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String id,

        @Schema(
                description = "Employee's  first name",
                example = "Homer"
        )
        String firstName,

        @Schema(
                description = "Employee's  last name",
                example = "Simpson"
        )
        String lastName,

        @Schema(
                description = "Employee's email",
                example = "homer@simpsons.com"
        )
        String email,

        @Schema(description = "Role granted to this Employee")
        String role,

        @Schema(description = "Organization ID", example = "7d7fb...")
        String organizationId
) {
}
