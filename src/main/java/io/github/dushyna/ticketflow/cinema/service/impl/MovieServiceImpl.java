package io.github.dushyna.ticketflow.cinema.service.impl;

import io.github.dushyna.ticketflow.cinema.dto.request.MovieCreateDto;
import io.github.dushyna.ticketflow.cinema.entity.Movie;
import io.github.dushyna.ticketflow.cinema.repository.MovieRepository;
import io.github.dushyna.ticketflow.cinema.service.interfaces.MovieService;
import io.github.dushyna.ticketflow.cinema.utils.MovieMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;

    @Override
    @Transactional
    public Movie createMovie(MovieCreateDto dto) {
        Movie movie = movieMapper.mapDtoToEntity(dto);
        return movieRepository.save(movie);
    }

    @Override
    @Transactional
    public Movie updateMovie(UUID id, MovieCreateDto dto) {
        Movie movie = getMovieById(id);

        movieMapper.updateEntityFromDto(dto, movie);

        return movieRepository.save(movie);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Movie getMovieById(UUID id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Movie not found with id: " + id));
    }

    @Override
    @Transactional
    public void deleteMovie(UUID id) {
        if (!movieRepository.existsById(id)) {
            throw new EntityNotFoundException("Movie not found");
        }
        movieRepository.deleteById(id);
    }
}
