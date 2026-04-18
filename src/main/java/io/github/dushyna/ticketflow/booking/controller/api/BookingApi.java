package io.github.dushyna.ticketflow.booking.controller.api;

import io.github.dushyna.ticketflow.booking.dto.request.BookingCreateDto;
import io.github.dushyna.ticketflow.booking.dto.response.BookingResponseDto;
import io.github.dushyna.ticketflow.booking.dto.response.SeatCoordinateDto;
import io.github.dushyna.ticketflow.exception.handling.response.ErrorResponseDto;
import io.github.dushyna.ticketflow.security.dto.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Booking Management", description = "Operations for booking seats and managing tickets")
@RequestMapping("/api/v1/bookings")
public interface BookingApi {

    @Operation(summary = "Create new seat bookings", description = "Books multiple seats for the current authenticated user.")
    @SecurityRequirement(name = "cookieAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Bookings created successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User must be logged in"),
            @ApiResponse(responseCode = "409", description = "Conflict - One or more seats are already taken",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    void create(
            @Valid @RequestBody BookingCreateDto dto,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails
    );

    @Operation(summary = "Get occupied seats", description = "Returns a list of taken row/col coordinates for a specific showtime.")
    @ApiResponse(responseCode = "200", description = "List of occupied seats retrieved",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = SeatCoordinateDto.class))))
    @GetMapping("/occupied/{showtimeId}")
    @PreAuthorize("permitAll()")
    List<SeatCoordinateDto> getOccupied(
            @Parameter(description = "ID of the specific showtime", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID showtimeId
    );

    @Operation(summary = "Get my bookings", description = "Returns a list of all tickets booked by the current user.")
    @SecurityRequirement(name = "cookieAuth")
    @ApiResponse(responseCode = "200", description = "User's booking history retrieved")
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    List<BookingResponseDto> getMyBookings(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails
    );
}
