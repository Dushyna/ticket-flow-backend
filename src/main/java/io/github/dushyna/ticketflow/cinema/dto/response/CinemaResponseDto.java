package io.github.dushyna.ticketflow.cinema.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Response DTO representing a cinema building")
public record CinemaResponseDto(
        @Schema(description = "Unique identifier of the cinema", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Name of the cinema", example = "Cinema Star Central")
        String name,

        @Schema(description = "Physical address of the cinema", example = "123 Movie Ave, New York, NY")
        String address,

        @Schema(description = "ID of the organization that owns this cinema")
        UUID organizationId,

        @Schema(description = "List of halls available in this cinema")
        List<MovieHallResponseDto> halls
) {}
