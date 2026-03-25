package io.github.dushyna.ticketflow.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Unified error response for security exceptions.
 */
@Schema(description = "Error response body")
public record ErrorResponse(
        @Schema(description = "Timestamp of the error occurrence", example = "2025-04-17T10:15:30Z")
        String timestamp,
        @Schema(description = "HTTP status code", example = "401")
        int status,
        @Schema(description = "Error", example = "Unauthorized")
        String error,
        @Schema(description = "Detailed error message", example = "Full authentication is required to access this resource.")
        String message,
        @Schema(description = "Request path", example = "/api/auth/login")
        String path
) {
}
