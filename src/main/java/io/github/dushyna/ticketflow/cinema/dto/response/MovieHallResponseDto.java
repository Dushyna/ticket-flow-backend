package io.github.dushyna.ticketflow.cinema.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Response DTO representing a movie hall layout")
public record MovieHallResponseDto(
        UUID id,
        String name,
        UUID cinemaId,
        Integer rowsCount,
        Integer colsCount,
        Map<String, Object> layoutConfig
) {}
