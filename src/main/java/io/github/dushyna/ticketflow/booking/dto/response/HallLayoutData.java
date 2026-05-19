package io.github.dushyna.ticketflow.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "Internal data transfer object representing parsed hall layout configuration")
public record HallLayoutData(
        @Schema(description = "Two-dimensional matrix representing the seat grid layout")
        List<List<String>> grid,

        @Schema(description = "List of price zones and configs associated with specific zone IDs")
        List<Map<String, Object>> zoneConfigs
) {}
