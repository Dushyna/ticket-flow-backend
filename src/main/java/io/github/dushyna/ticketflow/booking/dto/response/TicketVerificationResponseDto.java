package io.github.dushyna.ticketflow.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Response containing gate control verification results")
public record TicketVerificationResponseDto(
        @Schema(description = "True if entrance is permitted, false otherwise")
        boolean valid,

        @Schema(description = "Message explaining the verification result (e.g., Access Granted, Already Scanned)")
        String message,

        @Schema(description = "Title of the movie")
        String movieTitle,

        @Schema(description = "Row and Seat representation")
        String seatInfo,

        @Schema(description = "Showtime start time")
        Instant startTime
) {}
