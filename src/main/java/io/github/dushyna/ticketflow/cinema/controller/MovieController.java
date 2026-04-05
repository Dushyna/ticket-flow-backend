package io.github.dushyna.ticketflow.cinema.controller;

import io.github.dushyna.ticketflow.cinema.controller.api.MovieApi;
import io.github.dushyna.ticketflow.cinema.dto.request.MovieCreateDto;
import io.github.dushyna.ticketflow.cinema.entity.Movie;
import io.github.dushyna.ticketflow.cinema.service.interfaces.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MovieController implements MovieApi {

    private final MovieService movieService;

    @Override
    public Movie create(MovieCreateDto dto) {
        return movieService.createMovie(dto);
    }

    @Override
    public Movie update(UUID id, MovieCreateDto dto) {
        return movieService.updateMovie(id, dto);
    }

    @Override
    public List<Movie> getAll() {
        return movieService.getAllMovies();
    }

    @Override
    public Movie getById(UUID id) {
        return movieService.getMovieById(id);
    }

    @Override
    public void delete(UUID id) {
        movieService.deleteMovie(id);
    }

}
