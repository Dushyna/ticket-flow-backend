package io.github.dushyna.ticketflow.cinema.controller.api;

import io.github.dushyna.ticketflow.cinema.dto.request.MovieCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.MovieResponseDto;
import io.github.dushyna.ticketflow.security.dto.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@Tag(name = "Movie Management", description = "Operations for managing movie catalog")
@RequestMapping("/api/v1/movies")
public interface MovieApi {

    @Operation(summary = "Add a new movie", description = "Creates a movie entry. Accessible only by cinema owners.")
    @SecurityRequirement(name = "cookieAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Movie created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    MovieResponseDto create(@Valid @RequestBody MovieCreateDto dto,
                            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails);

    @Operation(summary = "Update movie details", description = "Updates an existing movie's information.")
    @SecurityRequirement(name = "cookieAuth")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    MovieResponseDto update(@Parameter(description = "UUID of the movie") @PathVariable UUID id,
                 @Valid @RequestBody MovieCreateDto dto,
                 @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails);

    @Operation(summary = "Get all movies", description = "Returns a list of all movies for viewers and owners")
    @GetMapping
    @PreAuthorize("permitAll()")
    List<MovieResponseDto> getAll(@Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails);

    @Operation(summary = "Get movie by ID")
    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    MovieResponseDto getById(@Parameter(description = "UUID of the movie") @PathVariable UUID id,
                  @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails);

    @Operation(summary = "Delete a movie", description = "Removes a movie from the catalog.")
    @SecurityRequirement(name = "cookieAuth")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    void delete(@Parameter(description = "UUID of the movie") @PathVariable UUID id,
                @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails);
}
