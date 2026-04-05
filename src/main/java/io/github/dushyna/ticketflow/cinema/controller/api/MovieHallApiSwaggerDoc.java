package io.github.dushyna.ticketflow.cinema.controller.api;

import io.github.dushyna.ticketflow.cinema.dto.request.MovieHallCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.MovieHallResponseDto;
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

@Tag(name = "Movie Hall Management", description = "Operations for creating and managing cinema hall layouts")
public interface MovieHallApiSwaggerDoc {

    @Operation(summary = "Create a new hall layout", description = "Saves rows, columns, and JSON configuration of the hall")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Hall created successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cinema belongs to another organization",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    MovieHallResponseDto create(MovieHallCreateDto dto);

    @Operation(summary = "Get all halls for a specific cinema")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MovieHallResponseDto.class))))
    List<MovieHallResponseDto> getAllByCinema(UUID cinemaId);

    @Operation(summary = "Get hall details by ID")
    @SecurityRequirement(name = "bearerAuth")
    MovieHallResponseDto getById(UUID id);

    @Operation(summary = "Update an existing hall layout")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hall updated successfully"),
            @ApiResponse(responseCode = "404", description = "Hall not found")
    })
    MovieHallResponseDto update(UUID id, MovieHallCreateDto dto);

    @Operation(summary = "Delete movie hall", description = "Permanently removes a hall from the cinema")
    void delete(UUID id);

}
