package io.github.dushyna.ticketflow.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Detailed information about a ticket booking")
public record BookingResponseDto(
        @Schema(description = "Unique identifier of the booking", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Title of the movie", example = "Inception")
        String movieTitle,

        @Schema(description = "Name of the cinema hall", example = "IMAX Hall 1")
        String hallName,

        @Schema(description = "Showtime start date and time")
        Instant startTime,

        @Schema(description = "Seat position in the hall grid")
        SeatCoordinateDto seat,

        @Schema(description = "Category of the ticket", example = "Student")
        String ticketLabel,

        @Schema(description = "Final price of the ticket", example = "12.50")
        BigDecimal price,

        @Schema(description = "Current booking status", example = "CONFIRMED")
        String status
) {}
