package io.github.dushyna.ticketflow.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Response data containing the secure Stripe integration checkout redirect URL payload")
public record PaymentResponseDto(
        @NotBlank
        @Schema(description = "The absolute secure redirect checkout session URL hosted by Stripe servers",
                example = "https://stripe.com")
        String paymentUrl
) {}
