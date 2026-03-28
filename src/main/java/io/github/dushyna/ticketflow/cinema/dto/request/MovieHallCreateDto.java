package io.github.dushyna.ticketflow.cinema.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Request DTO for creating a new Movie Hall layout")
public record MovieHallCreateDto(

        @NotBlank
        @Schema(description = "Name of the hall", example = "IMAX Premium")
        String name,

        @NotNull
        @Schema(description = "ID of the cinema this hall belongs to")
        UUID cinemaId,

        @Min(1)
        @Schema(description = "Total number of rows", example = "10")
        Integer rows,

        @Min(1)
        @Schema(description = "Total number of columns", example = "12")
        Integer cols,

        @NotNull
        @Schema(description = "Dynamic layout configuration including seat grid and zone definitions",
                example = "{\"grid\": [[\"z1\", \"aisle\"]], \"zoneConfigs\": [{\"id\": \"z1\", \"label\": \"VIP\"}]}")
        Map<String, Object> layoutConfig
) {}
