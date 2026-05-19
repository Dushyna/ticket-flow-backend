package io.github.dushyna.ticketflow.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Simple response containing the current status of an order")
public record BookingStatusResponseDto(
        @Schema(description = "Current status of the order", example = "CONFIRMED")
        String status
) {}
