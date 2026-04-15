package io.github.dushyna.ticketflow.booking.controller.api;

import io.github.dushyna.ticketflow.booking.dto.request.BookingCreateDto;
import io.github.dushyna.ticketflow.booking.dto.response.BookingResponseDto;
import io.github.dushyna.ticketflow.booking.dto.response.SeatCoordinateDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;

@Tag(name = "Booking Management", description = "Operations for booking seats in movie halls")
public interface BookingApiSwaggerDoc {

    @Operation(summary = "Create new seat bookings", description = "Books multiple seats for the current authenticated user")
    @ApiResponse(responseCode = "201", description = "Bookings created successfully")
    @ApiResponse(responseCode = "401", description = "User must be authenticated")
    @ApiResponse(responseCode = "409", description = "One or more seats are already taken")
    void create(
            BookingCreateDto dto,
            @Parameter(hidden = true) AppUser user
    );

    @Operation(summary = "Get occupied seats", description = "Returns a list of taken row/col coordinates for a specific showtime")
    @ApiResponse(responseCode = "200", description = "List of occupied seats retrieved")
    List<SeatCoordinateDto> getOccupied(
            @Parameter(description = "ID of the specific showtime", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID showtimeId
    );

    @Operation(summary = "Get user bookings", description = "Returns a list of all tickets booked by the current user")
    @ApiResponse(responseCode = "200", description = "User's booking history retrieved")
    List<BookingResponseDto> getMyBookings(@Parameter(hidden = true) AppUser user);

}
