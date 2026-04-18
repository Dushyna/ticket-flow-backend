package io.github.dushyna.ticketflow.cinema.service.impl;

import io.github.dushyna.ticketflow.cinema.dto.request.MovieCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.MovieResponseDto;
import io.github.dushyna.ticketflow.cinema.entity.Movie;
import io.github.dushyna.ticketflow.cinema.repository.MovieRepository;
import io.github.dushyna.ticketflow.cinema.service.interfaces.MovieService;
import io.github.dushyna.ticketflow.cinema.utils.MovieMapper;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.entity.Role;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
    public MovieResponseDto createMovie(MovieCreateDto dto, AppUser currentUser) {
        validateManagementAccess(currentUser);

        Movie movie = movieMapper.mapDtoToEntity(dto);
        Movie saved = movieRepository.save(movie);

        return movieMapper.mapEntityToResponseDto(saved);
    }

    @Override
    @Transactional
    public MovieResponseDto updateMovie(UUID id, MovieCreateDto dto, AppUser currentUser) {
        validateManagementAccess(currentUser);

        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Movie not found with id: " + id));

        movieMapper.updateEntityFromDto(dto, movie);
        Movie saved = movieRepository.save(movie);

        return movieMapper.mapEntityToResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieResponseDto> getAllMovies() {
        return movieRepository.findAll().stream()
                .map(movieMapper::mapEntityToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MovieResponseDto getMovieById(UUID id) {
        return movieRepository.findById(id)
                .map(movieMapper::mapEntityToResponseDto)
                .orElseThrow(() -> new EntityNotFoundException("Movie not found with id: " + id));
    }

    @Override
    @Transactional
    public void deleteMovie(UUID id, AppUser currentUser) {
        validateManagementAccess(currentUser);
        if (!movieRepository.existsById(id)) {
            throw new EntityNotFoundException("Movie not found");
        }
        movieRepository.deleteById(id);
    }

    private void validateManagementAccess(AppUser user) {
        if (user == null ||
                (user.getRole() != Role.ROLE_TENANT_ADMIN && user.getRole() != Role.ROLE_SUPER_ADMIN)) {
            throw new AccessDeniedException("Only cinema owners or admins can modify the movie catalog.");
        }
    }
}
