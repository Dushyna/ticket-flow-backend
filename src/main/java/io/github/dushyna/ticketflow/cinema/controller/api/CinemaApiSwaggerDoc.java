package io.github.dushyna.ticketflow.cinema.controller.api;

import io.github.dushyna.ticketflow.cinema.dto.request.CinemaCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.CinemaResponseDto;
import io.github.dushyna.ticketflow.exception.handling.response.ErrorResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cinema created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    CinemaResponseDto create(CinemaCreateDto dto);

    @Operation(
            summary = "Update cinema details",
            description = "Partially updates cinema information. Only provided fields will be changed.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Cinema successfully updated",
                            content = @Content(schema = @Schema(implementation = CinemaResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Forbidden: User does not have access to this cinema"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Cinema not found"
                    )
            }
    )
    CinemaResponseDto update(
            @Parameter(description = "UUID of the cinema to be updated") UUID id,
            CinemaCreateDto dto
    );

    @Operation(summary = "Get all cinemas of the organization")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CinemaResponseDto.class))))
    List<CinemaResponseDto> getAll();

    @Operation(summary = "Get cinema details by ID")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CinemaResponseDto.class)))
    CinemaResponseDto getById(UUID id);

    @Operation(
            summary = "Delete a cinema",
            description = "Deletes a specific cinema by its UUID. Only accessible if it belongs to the user's organization.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Cinema successfully deleted"),
                    @ApiResponse(responseCode = "403", description = "Forbidden: Access denied"),
                    @ApiResponse(responseCode = "404", description = "Cinema not found")
            }
    )
    void delete(@Parameter(description = "UUID of the cinema to be deleted") UUID id);

}
