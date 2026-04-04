package io.github.dushyna.ticketflow.booking.controller.api;

import io.github.dushyna.ticketflow.booking.dto.request.BookingCreateDto;
import io.github.dushyna.ticketflow.booking.dto.response.SeatCoordinateDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;

@Tag(name = "Booking Management", description = "Operations for booking seats in movie halls")
public interface BookingApiSwaggerDoc {

    @Operation(summary = "Create new seat bookings", description = "Books multiple seats for the current user")
    @ApiResponse(responseCode = "201", description = "Bookings created successfully")
    void create(BookingCreateDto dto);

    @Operation(summary = "Get occupied seats", description = "Returns a list of row/col coordinates that are already taken")
    @ApiResponse(responseCode = "200", description = "List of occupied seats retrieved")
    List<SeatCoordinateDto> getOccupied(UUID hallId);
}
