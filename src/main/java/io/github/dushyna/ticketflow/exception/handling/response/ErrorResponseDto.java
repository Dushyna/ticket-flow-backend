package io.github.dushyna.ticketflow.exception.handling.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response returned by all exception handlers.
 */
@Schema(description = "Standard API error response")
public record ErrorResponseDto(
        @Schema(
                description = "Timestamp when the error occurred",
                example = "2025-04-26T10:00:00"
        )
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,

        @Schema(
                description = "HTTP status code",
                example = "404"
        )
        int status,

        @Schema(
                description = "HTTP status reason phrase",
                example = "Not Found"
        )
        String error,

        @Schema(
                description = "Detailed error message",
                example = "User not found"
        )
        String message,

        @Schema(
                description = "Validation errors (if any)",
                implementation = ValidationErrorDto.class
        )
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<ValidationErrorDto> errors,

        @Schema(
                description = "URI path of the request that caused the error",
                example = "api/v1/users/42"
        )
        String path
) {
}
