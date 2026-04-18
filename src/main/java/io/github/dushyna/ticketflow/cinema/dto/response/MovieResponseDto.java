package io.github.dushyna.ticketflow.cinema.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Response DTO representing movie details for the catalog")
public record MovieResponseDto(
        @Schema(description = "Unique identifier of the movie", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Full title of the movie", example = "Inception")
        String title,

        @Schema(description = "Detailed plot summary", example = "A thief who steals corporate secrets through the use of dream-sharing technology...")
        String description,

        @Schema(description = "Movie duration in minutes", example = "148")
        Integer durationMinutes,

        @Schema(description = "URL to the movie poster image", example = "https://ticketflow.com")
        String posterUrl,

        @Schema(description = "Official release date", example = "2010-07-16")
        LocalDate releaseDate
) {}
