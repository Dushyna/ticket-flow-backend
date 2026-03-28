package io.github.dushyna.ticketflow.cinema.controller.api;

import io.github.dushyna.ticketflow.cinema.dto.request.CinemaCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.CinemaResponseDto;
import io.github.dushyna.ticketflow.exception.handling.response.ErrorResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Tag(name = "Cinema Management", description = "Operations for managing cinema buildings/locations")
public interface CinemaApiSwaggerDoc {

    @Operation(summary = "Create a new cinema", description = "Registers a new cinema building for the user's organization")
    @SecurityRequirement(name = "bearerAuth") // Вказує Swagger, що потрібна авторизація
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cinema created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    CinemaResponseDto create(CinemaCreateDto dto);

    @Operation(summary = "Get all cinemas of the organization")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CinemaResponseDto.class))))
    List<CinemaResponseDto> getAll();

    @Operation(summary = "Get cinema details by ID")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CinemaResponseDto.class)))
    CinemaResponseDto getById(UUID id);
}
