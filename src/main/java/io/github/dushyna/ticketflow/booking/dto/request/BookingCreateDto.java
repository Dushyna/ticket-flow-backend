package io.github.dushyna.ticketflow.booking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@Schema(description = "Request to book multiple seats in a movie hall")
public record BookingCreateDto(
        @NotNull(message = "{booking.showtime.required}")
        @Schema(description = "ID of the specific showtime", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID showtimeId,

        @NotEmpty(message = "{booking.seats.notEmpty}")
        @Schema(description = "List of coordinates for the selected seats")
        List<@Valid SeatCoordinateRequestDto> seats
) {}
