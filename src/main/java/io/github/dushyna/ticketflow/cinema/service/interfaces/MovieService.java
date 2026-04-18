package io.github.dushyna.ticketflow.cinema.service.interfaces;

import io.github.dushyna.ticketflow.cinema.dto.request.MovieCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.MovieResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;

import java.util.List;
import java.util.UUID;

public interface MovieService {
    MovieResponseDto createMovie(MovieCreateDto dto, AppUser currentUser);
    MovieResponseDto updateMovie(UUID id, MovieCreateDto dto, AppUser currentUser);
    List<MovieResponseDto> getAllMovies();
    MovieResponseDto getMovieById(UUID id);
    void deleteMovie(UUID id, AppUser currentUser);

}
