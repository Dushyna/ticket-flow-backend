package io.github.dushyna.ticketflow.cinema.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Response DTO representing a type of ticket with its discount multiplier")
public record TicketTypeResponseDto(
        @Schema(description = "Unique identifier of the ticket type", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Label of the ticket type", example = "Student")
        String label,

        @Schema(description = "Price multiplier for this ticket type", example = "0.80")
        BigDecimal discount,

        @Schema(description = "Whether this is the default ticket type selected in the UI", example = "false")
        boolean isDefault
) {}
