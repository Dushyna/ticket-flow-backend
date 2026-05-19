package io.github.dushyna.ticketflow.booking.controller.api;

import io.github.dushyna.ticketflow.booking.dto.request.BookingCreateDto;
import io.github.dushyna.ticketflow.booking.dto.response.*;
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

    @Operation(
            summary = "Create bookings and get payment URL",
            description = "Books multiple seats for the current authenticated user and returns a Stripe payment URL."
    )
    @SecurityRequirement(name = "cookieAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Bookings created successfully, payment URL generated",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User must be logged in"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - One or more seats are already taken",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error - Payment initialization failed"
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    PaymentResponseDto create(
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

    @Operation(summary = "Get order status by Stripe Session ID")
    @GetMapping("/status/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    BookingStatusResponseDto getStatusBySession(@PathVariable String sessionId);


    @Operation(summary = "Box Office Sale (Offline)", description = "Immediate ticket purchase by cashier.")
    @PostMapping("/box-office")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    BoxOfficeOrderResponseDto  sellAtBoxOffice(
            @Valid @RequestBody BookingCreateDto dto,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails
    );

    @Operation(summary = "Get payment URL for an existing pending order")
    @SecurityRequirement(name = "cookieAuth")
    @GetMapping("/payment-url/{orderId}")
    @PreAuthorize("isAuthenticated()")
    PaymentResponseDto getPaymentUrl(
            @Parameter(description = "ID of the existing order", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID orderId
    );

    @Operation(summary = "Verify ticket at the entrance gate via QR-code scan")
    @PostMapping("/verify-entrance/{bookingId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    TicketVerificationResponseDto verifyEntrance(@PathVariable UUID bookingId);

    @Operation(summary = "Get Cashier Sales History")
    @SecurityRequirement(name = "cookieAuth")
    @GetMapping("/cashier/history")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_CASHIER', 'ROLE_TENANT_ADMIN', 'ROLE_CONTROLLER')")
    List<CashierHistoryResponseDto> getCashierSalesHistory(
            @AuthenticationPrincipal AuthUserDetails userDetails
    );

}
