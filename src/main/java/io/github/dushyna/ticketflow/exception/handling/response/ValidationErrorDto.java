package io.github.dushyna.ticketflow.exception.handling.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Holds validation error messages for a single field.
 */
@Schema(description = "Details of a validation error for a specific field")
public record ValidationErrorDto(
        @Schema(
                description = "Name of the field with an error",
                example = "email"
        )
        String field,

        @Schema(
                description = "List of validation error messages for the field",
                example = "[\"must be a well-formed email address\"]"
        )
        List<String> messages
) {
}
