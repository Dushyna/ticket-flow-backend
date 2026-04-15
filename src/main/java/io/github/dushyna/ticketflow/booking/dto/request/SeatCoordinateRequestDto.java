package io.github.dushyna.ticketflow.booking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

import java.util.UUID;

@Schema(description = "Coordinates of a specific seat in the hall grid")
public record SeatCoordinateRequestDto(
        @Min(0)
        @Schema(description = "Zero-based row index", example = "5")
        int row,

        @Min(0)
        @Schema(description = "Zero-based column index", example = "12")
        int col,

        @Schema(description = "ID of the ticketType", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID ticketTypeId

) {}
