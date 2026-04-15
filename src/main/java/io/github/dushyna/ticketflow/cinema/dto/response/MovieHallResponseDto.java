package io.github.dushyna.ticketflow.cinema.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Response DTO representing a movie hall with its layout configuration")
public record MovieHallResponseDto(
        @Schema(description = "Unique identifier of the hall", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Name of the movie hall", example = "IMAX Grand Hall")
        String name,

        @Schema(description = "ID of the cinema building this hall belongs to", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
        UUID cinemaId,

        @Schema(description = "ID of the organization building this hall belongs to", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
        UUID organizationId,

        @Schema(description = "Number of rows in the seat grid", example = "10")
        Integer rowsCount,

        @Schema(description = "Number of columns in the seat grid", example = "12")
        Integer colsCount,

        @Schema(description = "JSON object containing grid zones and category configurations",
                example = "{\"grid\": [[\"parterre\", \"aisle\"]], \"zoneConfigs\": [{\"id\": \"parterre\", \"label\": \"Standard\", \"color\": \"#3b82f6\"}]}")
        Map<String, Object> layoutConfig
) {}
