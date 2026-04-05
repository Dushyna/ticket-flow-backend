package io.github.dushyna.ticketflow.cinema.controller.api;

import io.github.dushyna.ticketflow.cinema.dto.request.MovieCreateDto;
import io.github.dushyna.ticketflow.cinema.entity.Movie;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Tag(name = "Movie Management", description = "Operations for managing movie catalog")
public interface MovieApiSwaggerDoc {

    @Operation(summary = "Add a new movie", description = "Creates a movie entry with title, duration, and release info")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Movie created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    Movie create(MovieCreateDto dto);

    @Operation(
            summary = "Update movie details",
            description = "Updates an existing movie's information. Fields provided in the request body will replace current values.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Movie successfully updated",
                            content = @Content(schema = @Schema(implementation = Movie.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Movie not found"
                    )
            }
    )
    Movie update(
            @Parameter(description = "UUID of the movie to be updated") UUID id,
            MovieCreateDto dto
    );
    @Operation(summary = "Get all movies", description = "Returns a list of all movies in the system")
    @ApiResponse(responseCode = "200", description = "List of movies retrieved")
    List<Movie> getAll();

    @Operation(summary = "Get movie by ID")
    @ApiResponse(responseCode = "200", description = "Movie found")
    Movie getById(UUID id);

    @Operation(
            summary = "Delete a movie",
            description = "Removes a movie from the catalog by its ID. Warning: This may affect showtimes linked to this movie."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Movie deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Movie not found")
    })
    void delete(java.util.UUID id);
}
