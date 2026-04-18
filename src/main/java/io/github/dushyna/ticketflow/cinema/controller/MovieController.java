package io.github.dushyna.ticketflow.cinema.controller;

import io.github.dushyna.ticketflow.cinema.controller.api.MovieApi;
import io.github.dushyna.ticketflow.cinema.dto.request.MovieCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.MovieResponseDto;
import io.github.dushyna.ticketflow.cinema.service.interfaces.MovieService;
import io.github.dushyna.ticketflow.security.dto.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MovieController implements MovieApi {

    private final MovieService movieService;

    @Override
    public MovieResponseDto create(MovieCreateDto dto,
                                   @AuthenticationPrincipal AuthUserDetails userDetails) {
        return movieService.createMovie(dto, userDetails.user());
    }

    @Override
    public MovieResponseDto update(UUID id, MovieCreateDto dto,
                        @AuthenticationPrincipal AuthUserDetails userDetails) {
        return movieService.updateMovie(id, dto, userDetails.user());
    }

    @Override
    public List<MovieResponseDto> getAll(@AuthenticationPrincipal AuthUserDetails userDetails) {
        return movieService.getAllMovies();
    }

    @Override
    public MovieResponseDto getById(UUID id, @AuthenticationPrincipal AuthUserDetails userDetails) {
        return movieService.getMovieById(id);
    }

    @Override
    public void delete(UUID id, @AuthenticationPrincipal AuthUserDetails userDetails) {
        movieService.deleteMovie(id, userDetails.user());
    }
}
