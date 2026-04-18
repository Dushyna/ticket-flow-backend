package io.github.dushyna.ticketflow.cinema.controller.api;

import io.github.dushyna.ticketflow.cinema.dto.request.CinemaCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.CinemaResponseDto;
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

@Tag(name = "Cinema Management", description = "Operations for managing cinema buildings/locations")
@RequestMapping("/api/v1/cinemas")
public interface CinemaApi {

    @Operation(summary = "Create a new cinema", description = "Registers a new cinema building for the user's organization")
    @SecurityRequirement(name = "cookieAuth") // Змінив на cookieAuth, бо ми використовуємо куки
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cinema created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    CinemaResponseDto create(@Valid @RequestBody CinemaCreateDto dto,
                             @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails);

    @Operation(summary = "Update cinema details", description = "Partially updates cinema information.")
    @SecurityRequirement(name = "cookieAuth")
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    CinemaResponseDto update(
            @Parameter(description = "UUID of the cinema") @PathVariable UUID id,
            @Valid @RequestBody CinemaCreateDto dto,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails);

    @Operation(summary = "Get all cinemas", description = "Returns public list for guests or organization-specific list for owners")
    @GetMapping
    @PreAuthorize("permitAll()")
    List<CinemaResponseDto> getAll(@Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails);

    @Operation(summary = "Get cinema details by ID")
    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    CinemaResponseDto getById(
            @Parameter(description = "UUID of the cinema") @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails);

    @Operation(summary = "Delete a cinema")
    @SecurityRequirement(name = "cookieAuth")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    void delete(
            @Parameter(description = "UUID of the cinema") @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails);
}
