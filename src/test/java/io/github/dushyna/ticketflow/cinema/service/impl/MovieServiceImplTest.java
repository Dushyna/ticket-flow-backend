package io.github.dushyna.ticketflow.cinema.service.impl;

import io.github.dushyna.ticketflow.cinema.dto.request.MovieCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.MovieResponseDto;
import io.github.dushyna.ticketflow.cinema.entity.Movie;
import io.github.dushyna.ticketflow.cinema.repository.MovieRepository;
import io.github.dushyna.ticketflow.cinema.utils.MovieMapper;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.entity.Role;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for MovieServiceImpl")
class MovieServiceImplTest {

    @Mock private MovieRepository movieRepository;
    @Mock private MovieMapper movieMapper;

    @InjectMocks
    private MovieServiceImpl movieService;

    private AppUser adminUser;
    private AppUser regularUser;
    private Movie movie;
    private MovieCreateDto movieDto;

    @BeforeEach
    void setUp() {
        adminUser = new AppUser();
        adminUser.setRole(Role.ROLE_TENANT_ADMIN);

        regularUser = new AppUser();
        regularUser.setRole(Role.ROLE_USER);

        movie = new Movie();
        movie.setTitle("Interstellar");

        movieDto = new MovieCreateDto("Interstellar", "Desc", 169, "url", LocalDate.now());
    }

    // --- Create Movie ---

    @Test
    @DisplayName("createMovie: Success as Tenant Admin")
    void createMovie_Success() {
        given(movieMapper.mapDtoToEntity(movieDto)).willReturn(movie);
        given(movieRepository.save(movie)).willReturn(movie);
        given(movieMapper.mapEntityToResponseDto(movie)).willReturn(mock(MovieResponseDto.class));

        movieService.createMovie(movieDto, adminUser);

        verify(movieRepository).save(movie);
    }

    @Test
    @DisplayName("createMovie: Failure - Forbidden for ROLE_USER")
    void createMovie_Forbidden() {
        assertThatThrownBy(() -> movieService.createMovie(movieDto, regularUser))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(movieRepository);
    }

    // --- Update Movie ---

    @Test
    @DisplayName("updateMovie: Success")
    void updateMovie_Success() {
        UUID id = UUID.randomUUID();
        given(movieRepository.findById(id)).willReturn(Optional.of(movie));
        given(movieRepository.save(movie)).willReturn(movie);
        given(movieMapper.mapEntityToResponseDto(movie)).willReturn(mock(MovieResponseDto.class));

        movieService.updateMovie(id, movieDto, adminUser);

        verify(movieMapper).updateEntityFromDto(movieDto, movie);
        verify(movieRepository).save(movie);
    }

    @Test
    @DisplayName("updateMovie: Failure - Not Found")
    void updateMovie_NotFound() {
        UUID id = UUID.randomUUID();
        given(movieRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.updateMovie(id, movieDto, adminUser))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // --- Get Movies ---

    @Test
    @DisplayName("getAllMovies: Success")
    void getAllMovies_Success() {
        given(movieRepository.findAll()).willReturn(List.of(movie));
        given(movieMapper.mapEntityToResponseDto(movie)).willReturn(mock(MovieResponseDto.class));

        List<MovieResponseDto> result = movieService.getAllMovies();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getAllMovies: Edge Case - should return empty list when no movies in DB")
    void getAllMovies_EmptyDatabase_ReturnsEmptyList() {
        // Given
        given(movieRepository.findAll()).willReturn(java.util.List.of());

        // When
        List<MovieResponseDto> result = movieService.getAllMovies();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(movieRepository).findAll();
    }

    @Test
    @DisplayName("getMovieById: Success")
    void getMovieById_Success() {
        UUID id = UUID.randomUUID();
        given(movieRepository.findById(id)).willReturn(Optional.of(movie));
        given(movieMapper.mapEntityToResponseDto(movie)).willReturn(mock(MovieResponseDto.class));

        MovieResponseDto result = movieService.getMovieById(id);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getMovieById: Edge Case - should throw exception for non-existent UUID")
    void getMovieById_NotFound_ThrowsException() {
        // Given
        UUID randomId = UUID.randomUUID();
        given(movieRepository.findById(randomId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> movieService.getMovieById(randomId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Movie not found with id: " + randomId);
    }

    // --- Delete Movie ---

    @Test
    @DisplayName("deleteMovie: Success")
    void deleteMovie_Success() {
        UUID id = UUID.randomUUID();
        given(movieRepository.existsById(id)).willReturn(true);

        movieService.deleteMovie(id, adminUser);

        verify(movieRepository).deleteById(id);
    }

    @Test
    @DisplayName("deleteMovie: Failure - Not Found")
    void deleteMovie_NotFound() {
        UUID id = UUID.randomUUID();
        given(movieRepository.existsById(id)).willReturn(false);

        assertThatThrownBy(() -> movieService.deleteMovie(id, adminUser))
                .isInstanceOf(EntityNotFoundException.class);
        verify(movieRepository, never()).deleteById(any());
    }
}
