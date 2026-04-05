package io.github.dushyna.ticketflow.cinema.controller.api;

import io.github.dushyna.ticketflow.cinema.dto.request.ShowtimeCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.ShowtimeResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;

@Tag(name = "Showtime Management", description = "Operations for scheduling movies in halls")
public interface ShowtimeApiSwaggerDoc {

    @Operation(summary = "Schedule a new showtime", description = "Creates a session for a movie in a specific hall. Automatically calculates end time.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Showtime created successfully"),
            @ApiResponse(responseCode = "409", description = "Conflict - Hall is already occupied at this time"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User not authorized")
    })
    ShowtimeResponseDto create(ShowtimeCreateDto dto);

    @Operation(summary = "Update an existing showtime", description = "Updates details of a specific showtime. Requires appropriate permissions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Showtime updated successfully"),
            @ApiResponse(responseCode = "404", description = "Showtime not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - New timing overlaps with another session")
    })
    ShowtimeResponseDto update(UUID id, ShowtimeCreateDto dto);

    @Operation(summary = "Delete a showtime", description = "Removes a scheduled session from the system.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Showtime deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Showtime not found")
    })
    void delete(UUID id);

    @Operation(summary = "Get showtime by ID", description = "Returns detailed information about a single showtime.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Showtime found"),
            @ApiResponse(responseCode = "404", description = "Showtime not found")
    })
    ShowtimeResponseDto getById(UUID id);

    @Operation(summary = "Get all showtimes for a movie")
    @ApiResponse(responseCode = "200", description = "List of showtimes retrieved")
    List<ShowtimeResponseDto> getByMovie(UUID movieId);

    @Operation(summary = "Get all showtimes for a hall")
    @ApiResponse(responseCode = "200", description = "List of showtimes retrieved")
    List<ShowtimeResponseDto> getByHall(UUID hallId);
}
