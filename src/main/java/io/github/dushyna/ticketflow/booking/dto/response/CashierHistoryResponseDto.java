package io.github.dushyna.ticketflow.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Historical box office sale entry for cashier auditing and layout reprints")
public record CashierHistoryResponseDto(
        @NotNull
        @Schema(description = "Unique identifier of the completed order", example = "93476ae4-c746-4cf1-928a-de45f0bb17d6")
        UUID orderId,

        @NotBlank
        @Schema(description = "Title of the movie", example = "Interstellar")
        String movieTitle,

        @NotBlank
        @Schema(description = "Name of the movie hall", example = "IMAX Hall 1")
        String hallName,

        @Min(1)
        @Schema(description = "Total number of tickets in the order", example = "3")
        int ticketsCount,

        @NotNull
        @Schema(description = "Total financial price of the order", example = "45.50")
        BigDecimal totalPrice,

        @NotNull
        @Schema(description = "Timestamp when the order was successfully completed", example = "2026-05-16T20:12:22Z")
        Instant createdAt
) {}
