package io.github.dushyna.ticketflow.cinema.service.interfaces;

import io.github.dushyna.ticketflow.cinema.dto.request.MovieCreateDto;
import io.github.dushyna.ticketflow.cinema.entity.Movie;
import java.util.List;
import java.util.UUID;

public interface MovieService {
    Movie createMovie(MovieCreateDto dto);
    Movie updateMovie(UUID id, MovieCreateDto dto);
    List<Movie> getAllMovies();
    Movie getMovieById(UUID id);
    void deleteMovie(UUID id);

}
