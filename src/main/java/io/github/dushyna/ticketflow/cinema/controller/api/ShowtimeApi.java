package io.github.dushyna.ticketflow.cinema.controller.api;

import io.github.dushyna.ticketflow.cinema.dto.request.ShowtimeCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.ShowtimeResponseDto;
import io.github.dushyna.ticketflow.exception.handling.response.ErrorResponseDto;
import io.github.dushyna.ticketflow.security.dto.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@Tag(name = "Showtime Management", description = "Operations for scheduling movies in halls")
@RequestMapping("/api/v1/showtimes")
public interface ShowtimeApi {

    @Operation(summary = "Schedule a new showtime", description = "Creates a session for a movie in a specific hall.")
    @SecurityRequirement(name = "cookieAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Showtime created successfully"),
            @ApiResponse(responseCode = "409", description = "Conflict - Hall is already occupied at this time",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    ShowtimeResponseDto create(@Valid @RequestBody ShowtimeCreateDto dto,
                               @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails);

    @Operation(summary = "Update an existing showtime")
    @SecurityRequirement(name = "cookieAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Showtime updated successfully"),
            @ApiResponse(responseCode = "404", description = "Showtime not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - New timing overlaps with another session")
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    ShowtimeResponseDto update(@Parameter(description = "UUID of the showtime") @PathVariable UUID id,
                               @Valid @RequestBody ShowtimeCreateDto dto,
                               @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails);

    @Operation(summary = "Delete a showtime")
    @SecurityRequirement(name = "cookieAuth")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    void delete(@Parameter(description = "UUID of the showtime") @PathVariable UUID id,
                @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails);

    @Operation(summary = "Get showtime by ID")
    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    ShowtimeResponseDto getById(@Parameter(description = "UUID of the showtime") @PathVariable UUID id,
                                @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails);

    @Operation(summary = "Get all showtimes for a movie")
    @GetMapping("/movie/{movieId}")
    @PreAuthorize("permitAll()")
    List<ShowtimeResponseDto> getByMovie(@PathVariable UUID movieId);

    @Operation(summary = "Get all showtimes for a hall")
    @GetMapping("/hall/{hallId}")
    @PreAuthorize("permitAll()")
    List<ShowtimeResponseDto> getByHall(@PathVariable UUID hallId);

    @Operation(summary = "Get all showtimes for a cinema")
    @GetMapping("/cinema/{cinemaId}")
    @PreAuthorize("permitAll()")
    List<ShowtimeResponseDto> getByCinema(@PathVariable UUID cinemaId);
}
