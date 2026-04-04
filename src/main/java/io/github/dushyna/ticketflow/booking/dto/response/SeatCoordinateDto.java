package io.github.dushyna.ticketflow.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "Coordinates of a specific seat in the hall grid")
public record SeatCoordinateDto(
        @Min(0)
        @Schema(description = "Zero-based row index", example = "5")
        int row,

        @Min(0)
        @Schema(description = "Zero-based column index", example = "12")
        int col
) {}
