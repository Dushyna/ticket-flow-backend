package io.github.dushyna.ticketflow.cinema.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Request to create a new movie showtime")
public record ShowtimeCreateDto(
        @NotNull
        @Schema(description = "ID of the movie", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID movieId,

        @NotNull
        @Schema(description = "ID of the movie hall", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
        UUID hallId,

        @NotNull
        @Schema(description = "Start time of the showtime in UTC", example = "2024-05-10T18:30:00Z")
        Instant startTime,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        @Schema(description = "Base ticket price for this showtime", example = "150.00")
        BigDecimal basePrice
) {}
