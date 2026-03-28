package io.github.dushyna.ticketflow.cinema.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to create a new cinema building")
public record CinemaCreateDto(
        @NotBlank
        @Schema(example = "Cinema Star Central")
        String name,

        @NotBlank
        @Schema(example = "123 Movie Ave, New York, NY")
        String address
) {}
