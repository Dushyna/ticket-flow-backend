package io.github.dushyna.ticketflow.cinema.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Request DTO for creating or updating a ticket type")
public record TicketTypeRequestDto(
        @Schema(description = "Name of the ticket category", example = "Child")
        @NotBlank(message = "Label is required")
        String label,

        @Schema(description = "Price multiplier (e.g., 0.50 for 50% discount, 1.00 for no discount)", example = "0.50")
        @NotNull(message = "Discount multiplier is required")
        @DecimalMin(value = "0.0", message = "Discount cannot be less than 0")
        @DecimalMax(value = "10.0", message = "Discount cannot be more than 10")
        BigDecimal discount,

        @Schema(description = "Set as default selection for the booking page", example = "false")
        boolean isDefault
) {}
