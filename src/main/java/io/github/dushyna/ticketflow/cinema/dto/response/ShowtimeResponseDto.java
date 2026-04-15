package io.github.dushyna.ticketflow.cinema.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Response object representing a scheduled movie showtime")
public record ShowtimeResponseDto(
        @Schema(description = "Unique identifier of the showtime", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Unique identifier of the scheduled movie", example = "550e8402-e29b-41d4-a716-446655440000")
        UUID movieId,

        @Schema(description = "Title of the scheduled movie", example = "Interstellar")
        String movieTitle,

        @Schema(description = "Unique identifier of the hall", example = "550e8402-e29b-41d4-a716-446655440001")
        UUID hallId,

        @Schema(description = "Name of the movie hall", example = "IMAX Hall 1")
        String hallName,

        @Schema(description = "Start time of the showtime in UTC", example = "2024-05-10T18:30:00Z")
        Instant startTime,

        @Schema(description = "Calculated end time of the showtime (including cleaning buffer)", example = "2024-05-10T21:15:00Z")
        Instant endTime,

        @Schema(description = "Ticket price for this session", example = "150.00")
        BigDecimal basePrice
) {}
