package io.github.dushyna.ticketflow.cinema.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Request to add a new movie to the system")
public record MovieCreateDto(
        @NotBlank @Schema(example = "Interstellar") String title,
        @Schema(example = "A team of explorers travel through a wormhole...") String description,
        @NotNull @Min(1) @Schema(example = "169") Integer durationMinutes,
        @Schema(example = "https://images.com") String posterUrl,
        @Schema(example = "2014-11-07") LocalDate releaseDate
) {}
