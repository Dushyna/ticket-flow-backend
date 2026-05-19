package io.github.dushyna.ticketflow.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@Schema(description = "Response data containing generated reference identifiers after a successful box office cash purchase")
public record BoxOfficeOrderResponseDto(
        @NotNull
        @Schema(description = "Unique identifier of the completed core order transaction", example = "a2d1f93c-5132-40bf-a971-416e95b260bd")
        UUID orderId,

        @NotEmpty
        @Schema(description = "List of unique identifiers for each individual ticket booking entry within the order",
                example = "[\"2449293c-5132-40bf-a971-416e95b260bd\", \"3e4c5918-3563-4a1d-8cfd-a2dd7480cff9\"]")
        List<UUID> bookingIds
) {}
