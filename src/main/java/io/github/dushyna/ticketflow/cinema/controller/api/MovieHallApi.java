package io.github.dushyna.ticketflow.cinema.controller.api;

import io.github.dushyna.ticketflow.cinema.dto.request.MovieHallCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.MovieHallResponseDto;
import io.github.dushyna.ticketflow.exception.handling.response.ErrorResponseDto;
import io.github.dushyna.ticketflow.security.dto.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Movie Hall Management", description = "Operations for creating and managing cinema hall layouts")
@RequestMapping("/api/v1/halls")
public interface MovieHallApi {

    @Operation(summary = "Create a new hall layout", description = "Saves rows, columns, and JSON configuration of the hall")
    @SecurityRequirement(name = "cookieAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Hall created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cinema belongs to another organization",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    MovieHallResponseDto create(
            @Valid @RequestBody MovieHallCreateDto dto,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails
    );

    @Operation(summary = "Get all halls for a specific cinema")
    @SecurityRequirement(name = "cookieAuth")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MovieHallResponseDto.class))))
    @GetMapping("/cinema/{cinemaId}")
    @PreAuthorize("permitAll()")
    List<MovieHallResponseDto> getAllByCinema(
            @Parameter(description = "UUID of the cinema") @PathVariable UUID cinemaId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails
    );

    @Operation(summary = "Get hall details by ID")
    @SecurityRequirement(name = "cookieAuth")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MovieHallResponseDto.class)))
    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    MovieHallResponseDto getById(
            @Parameter(description = "UUID of the hall") @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails
    );

    @Operation(summary = "Update an existing hall layout")
    @SecurityRequirement(name = "cookieAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hall updated successfully"),
            @ApiResponse(responseCode = "404", description = "Hall not found")
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    MovieHallResponseDto update(
            @Parameter(description = "UUID of the hall") @PathVariable UUID id,
            @Valid @RequestBody MovieHallCreateDto dto,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails
    );

    @Operation(summary = "Delete movie hall", description = "Permanently removes a hall from the cinema")
    @SecurityRequirement(name = "cookieAuth")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    void delete(
            @Parameter(description = "UUID of the hall") @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails
    );
}
