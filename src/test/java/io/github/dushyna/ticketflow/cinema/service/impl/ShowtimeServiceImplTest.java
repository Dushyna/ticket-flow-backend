package io.github.dushyna.ticketflow.cinema.service.impl;

import io.github.dushyna.ticketflow.cinema.dto.request.ShowtimeCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.ShowtimeResponseDto;
import io.github.dushyna.ticketflow.cinema.entity.Cinema;
import io.github.dushyna.ticketflow.cinema.entity.Movie;
import io.github.dushyna.ticketflow.cinema.entity.MovieHall;
import io.github.dushyna.ticketflow.cinema.entity.Showtime;
import io.github.dushyna.ticketflow.cinema.repository.MovieHallRepository;
import io.github.dushyna.ticketflow.cinema.repository.MovieRepository;
import io.github.dushyna.ticketflow.cinema.repository.ShowtimeRepository;
import io.github.dushyna.ticketflow.cinema.utils.ShowtimeMapper;
import io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException;
import io.github.dushyna.ticketflow.organization.entity.Organization;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for ShowtimeServiceImpl")
class ShowtimeServiceImplTest {

    @Mock private ShowtimeRepository showtimeRepository;
    @Mock private MovieRepository movieRepository;
    @Mock private MovieHallRepository hallRepository;
    @Mock private ShowtimeMapper showtimeMapper;

    @InjectMocks
    private ShowtimeServiceImpl showtimeService;

    private AppUser tenantAdmin;
    private Movie movie;
    private MovieHall hall;

    @BeforeEach
    void setUp() {
        Organization organization = new Organization();
        ReflectionTestUtils.setField(organization, "id", UUID.randomUUID());

        tenantAdmin = new AppUser();
        tenantAdmin.setRole(Role.ROLE_TENANT_ADMIN);
        tenantAdmin.setOrganization(organization);

        movie = new Movie();
        movie.setDurationMinutes(100);

        Cinema cinema = new Cinema();
        cinema.setOrganization(organization);
        hall = new MovieHall();
        hall.setCinema(cinema);
        ReflectionTestUtils.setField(hall, "id", UUID.randomUUID());
    }

    @Test
    @DisplayName("createShowtime: Success - calculate end time with 15min cleaning buffer")
    void createShowtime_Success() {
        // Given
        Instant start = Instant.now().plus(Duration.ofDays(1));
        ShowtimeCreateDto dto = new ShowtimeCreateDto(UUID.randomUUID(), hall.getId(), start, BigDecimal.valueOf(100));
        Showtime showtime = new Showtime();

        given(movieRepository.findById(any())).willReturn(Optional.of(movie));
        given(hallRepository.findById(any())).willReturn(Optional.of(hall));
        given(showtimeRepository.existsOverlappingShowtime(any(), any(), any(), any())).willReturn(false);
        given(showtimeMapper.mapCreateDtoToEntity(dto)).willReturn(showtime);
        given(showtimeRepository.save(any())).willReturn(showtime);
        given(showtimeMapper.mapEntityToResponseDto(any())).willReturn(mock(ShowtimeResponseDto.class));

        // When
        showtimeService.createShowtime(dto, tenantAdmin);

        // Then
        // 100 (movie) + 15 (buffer) = 115 min
        Instant expectedEnd = start.plus(Duration.ofMinutes(115));
        assertThat(showtime.getEndTime()).isEqualTo(expectedEnd);
        verify(showtimeRepository).save(showtime);
    }

    @Test
    @DisplayName("createShowtime: Failure - Showtime in the past")
    void createShowtime_PastTime_ThrowsException() {
        // Given
        Instant pastStart = Instant.now().minus(Duration.ofHours(1));
        ShowtimeCreateDto dto = new ShowtimeCreateDto(UUID.randomUUID(), hall.getId(), pastStart, BigDecimal.valueOf(100));

        given(movieRepository.findById(any())).willReturn(Optional.of(movie));
        given(hallRepository.findById(any())).willReturn(Optional.of(hall));

        // When & Then
        assertThatThrownBy(() -> showtimeService.createShowtime(dto, tenantAdmin))
                .isInstanceOf(RestApiException.class)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("Cannot schedule a showtime in the past");
    }

    @Test
    @DisplayName("createShowtime: Failure - Conflict with another showtime")
    void createShowtime_Conflict_ThrowsException() {
        // Given
        Instant start = Instant.now().plus(Duration.ofDays(1));
        ShowtimeCreateDto dto = new ShowtimeCreateDto(UUID.randomUUID(), hall.getId(), start, BigDecimal.valueOf(100));

        given(movieRepository.findById(any())).willReturn(Optional.of(movie));
        given(hallRepository.findById(any())).willReturn(Optional.of(hall));
        // Simulate overlap found
        given(showtimeRepository.existsOverlappingShowtime(any(), any(), any(), isNull())).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> showtimeService.createShowtime(dto, tenantAdmin))
                .isInstanceOf(RestApiException.class)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.CONFLICT)
                .hasMessageContaining("already booked");
    }

    @Test
    @DisplayName("createShowtime: Failure - Access denied to another org's hall")
    void createShowtime_NoAccess_ThrowsException() {
        // Given
        Organization otherOrg = new Organization();
        ReflectionTestUtils.setField(otherOrg, "id", UUID.randomUUID());
        hall.getCinema().setOrganization(otherOrg);

        ShowtimeCreateDto dto = new ShowtimeCreateDto(UUID.randomUUID(), hall.getId(), Instant.now().plus(Duration.ofDays(1)), BigDecimal.ONE);

        given(movieRepository.findById(any())).willReturn(Optional.of(movie));
        given(hallRepository.findById(any())).willReturn(Optional.of(hall));

        // When & Then
        assertThatThrownBy(() -> showtimeService.createShowtime(dto, tenantAdmin))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No access to this hall");
    }

    @Test
    @DisplayName("updateShowtime: Success - should update showtime and ignore self in conflict check")
    void updateShowtime_Success() {
        // Given
        UUID showtimeId = UUID.randomUUID();
        Instant newStart = Instant.now().plus(Duration.ofDays(2));
        ShowtimeCreateDto dto = new ShowtimeCreateDto(movie.getId(), hall.getId(), newStart, BigDecimal.valueOf(200));

        Showtime existingShowtime = new Showtime();
        existingShowtime.setHall(hall);
        existingShowtime.setMovie(movie);
        existingShowtime.setStartTime(Instant.now().plus(Duration.ofDays(1)));

        given(showtimeRepository.findById(showtimeId)).willReturn(Optional.of(existingShowtime));
        given(showtimeRepository.existsOverlappingShowtime(eq(hall.getId()), any(), any(), eq(showtimeId))).willReturn(false);
        given(showtimeRepository.save(any())).willReturn(existingShowtime);
        given(showtimeMapper.mapEntityToResponseDto(any())).willReturn(mock(ShowtimeResponseDto.class));

        // When
        showtimeService.updateShowtime(showtimeId, dto, tenantAdmin);

        // Then
        // Verify that the conflict check excluded the current showtime ID
        verify(showtimeRepository).existsOverlappingShowtime(eq(hall.getId()), any(), any(), eq(showtimeId));
        verify(showtimeRepository).save(existingShowtime);
    }

    @Test
    @DisplayName("updateShowtime: Success - should recalculate end time when movie is changed")
    void updateShowtime_ChangeMovie_RecalculatesTime() {
        // Given
        UUID showtimeId = UUID.randomUUID();
        Movie newMovie = new Movie();
        newMovie.setDurationMinutes(200); // Longer movie

        ShowtimeCreateDto dto = new ShowtimeCreateDto(UUID.randomUUID(), hall.getId(), Instant.now().plus(Duration.ofDays(1)), BigDecimal.TEN);
        Showtime existingShowtime = new Showtime();
        existingShowtime.setHall(hall);
        existingShowtime.setMovie(movie); // old movie (100 min)
        existingShowtime.setStartTime(Instant.now().plus(Duration.ofDays(1)));

        given(showtimeRepository.findById(showtimeId)).willReturn(Optional.of(existingShowtime));
        given(movieRepository.findById(any())).willReturn(Optional.of(newMovie));
        given(showtimeRepository.save(any())).willReturn(existingShowtime);
        given(showtimeMapper.mapEntityToResponseDto(any())).willReturn(mock(ShowtimeResponseDto.class));

        // When
        showtimeService.updateShowtime(showtimeId, dto, tenantAdmin);

        // Then
        // 200 (new movie) + 15 (buffer) = 215 min
        Instant expectedEnd = existingShowtime.getStartTime().plus(Duration.ofMinutes(215));
        assertThat(existingShowtime.getEndTime()).isEqualTo(expectedEnd);
        assertThat(existingShowtime.getMovie()).isEqualTo(newMovie);
    }

    @Test
    @DisplayName("updateShowtime: Failure - Conflict with another showtime")
    void updateShowtime_Conflict_ThrowsException() {
        // Given
        UUID showtimeId = UUID.randomUUID();
        Instant start = Instant.now().plus(Duration.ofDays(1));
        ShowtimeCreateDto dto = new ShowtimeCreateDto(movie.getId(), hall.getId(), start, BigDecimal.ONE);
        Showtime existingShowtime = new Showtime();
        existingShowtime.setHall(hall);
        existingShowtime.setMovie(movie);
        existingShowtime.setStartTime(start);

        given(showtimeRepository.findById(showtimeId)).willReturn(Optional.of(existingShowtime));
        // Simulate that another showtime occupies this slot
        given(showtimeRepository.existsOverlappingShowtime(any(), any(), any(), eq(showtimeId))).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> showtimeService.updateShowtime(showtimeId, dto, tenantAdmin))
                .isInstanceOf(RestApiException.class)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("updateShowtime: Failure - Showtime not found")
    void updateShowtime_NotFound_ThrowsException() {
        // Given
        UUID fakeId = UUID.randomUUID();
        given(showtimeRepository.findById(fakeId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> showtimeService.updateShowtime(fakeId, mock(ShowtimeCreateDto.class), tenantAdmin))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("updateShowtime: Success - Super Admin can update any showtime")
    void updateShowtime_SuperAdmin_Success() {
        // Given
        UUID showtimeId = UUID.randomUUID();
        AppUser superAdmin = new AppUser();
        superAdmin.setRole(Role.ROLE_SUPER_ADMIN);

        Showtime existingShowtime = new Showtime();
        existingShowtime.setHall(new MovieHall());
        existingShowtime.setMovie(movie); // ЦЬОГО РЯДКА НЕ ВИСТАЧАЛО ТУТ
        existingShowtime.setStartTime(Instant.now().plus(Duration.ofDays(1)));

        given(showtimeRepository.findById(showtimeId)).willReturn(Optional.of(existingShowtime));
        given(showtimeRepository.save(any())).willReturn(existingShowtime);
        given(showtimeMapper.mapEntityToResponseDto(any())).willReturn(mock(ShowtimeResponseDto.class));

        // When
        showtimeService.updateShowtime(showtimeId, mock(ShowtimeCreateDto.class), superAdmin);

        // Then
        verify(showtimeRepository).save(existingShowtime);
    }

    @Test
    @DisplayName("getByIdOrThrow: Success - should return showtime DTO when id exists")
    void getByIdOrThrow_Success() {
        // Given
        UUID id = UUID.randomUUID();
        Showtime showtime = new Showtime();
        ShowtimeResponseDto expectedDto = mock(ShowtimeResponseDto.class);

        given(showtimeRepository.findById(id)).willReturn(Optional.of(showtime));
        given(showtimeMapper.mapEntityToResponseDto(showtime)).willReturn(expectedDto);

        // When
        ShowtimeResponseDto result = showtimeService.getByIdOrThrow(id);

        // Then
        assertThat(result).isEqualTo(expectedDto);
        verify(showtimeRepository).findById(id);
    }

    @Test
    @DisplayName("getByIdOrThrow: Edge Case - should throw exception when id does not exist")
    void getByIdOrThrow_NotFound_ThrowsException() {
        // Given
        UUID fakeId = UUID.randomUUID();
        given(showtimeRepository.findById(fakeId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> showtimeService.getByIdOrThrow(fakeId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Showtime not found");
    }

    @Test
    @DisplayName("getShowtimesByHall: Success - should return list of showtimes for hall")
    void getShowtimesByHall_Success() {
        // Given
        UUID hallId = UUID.randomUUID();
        Showtime showtime = new Showtime();
        ShowtimeResponseDto responseDto = mock(ShowtimeResponseDto.class);

        given(showtimeRepository.findAllByHallId(hallId)).willReturn(List.of(showtime));
        given(showtimeMapper.mapEntityToResponseDto(showtime)).willReturn(responseDto);

        // When
        List<ShowtimeResponseDto> result = showtimeService.getShowtimesByHall(hallId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(responseDto);
        verify(showtimeRepository).findAllByHallId(hallId);
    }

    @Test
    @DisplayName("getShowtimesByHall: Edge Case - should return empty list when no showtimes found")
    void getShowtimesByHall_EmptyList() {
        // Given
        UUID hallId = UUID.randomUUID();
        given(showtimeRepository.findAllByHallId(hallId)).willReturn(List.of());

        // When
        List<ShowtimeResponseDto> result = showtimeService.getShowtimesByHall(hallId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(showtimeRepository).findAllByHallId(hallId);
    }

    @Test
    @DisplayName("getShowtimesByMovie: Success - should return list of showtimes for movie")
    void getShowtimesByMovie_Success() {
        // Given
        UUID movieId = UUID.randomUUID();
        Showtime showtime = new Showtime();
        ShowtimeResponseDto responseDto = mock(ShowtimeResponseDto.class);

        given(showtimeRepository.findAllByMovieId(movieId)).willReturn(List.of(showtime));
        given(showtimeMapper.mapEntityToResponseDto(showtime)).willReturn(responseDto);

        // When
        List<ShowtimeResponseDto> result = showtimeService.getShowtimesByMovie(movieId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(responseDto);
        verify(showtimeRepository).findAllByMovieId(movieId);
    }

    @Test
    @DisplayName("getShowtimesByMovie: Edge Case - should return empty list when no showtimes found for movie")
    void getShowtimesByMovie_EmptyList() {
        // Given
        UUID movieId = UUID.randomUUID();
        given(showtimeRepository.findAllByMovieId(movieId)).willReturn(List.of());

        // When
        List<ShowtimeResponseDto> result = showtimeService.getShowtimesByMovie(movieId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(showtimeRepository).findAllByMovieId(movieId);
    }

    @Test
    @DisplayName("getShowtimesByCinema: Success - should return showtimes with details for cinema")
    void getShowtimesByCinema_Success() {
        // Given
        UUID cinemaId = UUID.randomUUID();
        Showtime showtime = new Showtime();
        ShowtimeResponseDto responseDto = mock(ShowtimeResponseDto.class);

        given(showtimeRepository.findAllByCinemaIdWithDetails(cinemaId))
                .willReturn(List.of(showtime));
        given(showtimeMapper.mapEntityToResponseDto(showtime))
                .willReturn(responseDto);

        // When
        List<ShowtimeResponseDto> result = showtimeService.getShowtimesByCinema(cinemaId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(responseDto);
        verify(showtimeRepository).findAllByCinemaIdWithDetails(cinemaId);
    }

    @Test
    @DisplayName("getShowtimesByCinema: Edge Case - should return empty list when no showtimes for cinema")
    void getShowtimesByCinema_EmptyList() {
        // Given
        UUID cinemaId = UUID.randomUUID();
        given(showtimeRepository.findAllByCinemaIdWithDetails(cinemaId))
                .willReturn(List.of());

        // When
        List<ShowtimeResponseDto> result = showtimeService.getShowtimesByCinema(cinemaId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(showtimeRepository).findAllByCinemaIdWithDetails(cinemaId);
    }

    @Test
    @DisplayName("deleteShowtime: Success")
    void deleteShowtime_Success() {
        // Given
        UUID id = UUID.randomUUID();
        Showtime showtime = new Showtime();
        showtime.setHall(hall);

        given(showtimeRepository.findById(id)).willReturn(Optional.of(showtime));

        // When
        showtimeService.deleteShowtime(id, tenantAdmin);

        // Then
        verify(showtimeRepository).delete(showtime);
    }

    @Test
    @DisplayName("deleteShowtime: Failure - Forbidden to delete showtime of another organization")
    void deleteShowtime_Forbidden_ThrowsException() {
        // Given
        UUID showtimeId = UUID.randomUUID();

        // Showtime belongs to a DIFFERENT organization
        Organization otherOrg = new Organization();
        ReflectionTestUtils.setField(otherOrg, "id", UUID.randomUUID());

        Cinema otherCinema = new Cinema();
        otherCinema.setOrganization(otherOrg);

        MovieHall otherHall = new MovieHall();
        otherHall.setCinema(otherCinema);

        Showtime showtime = new Showtime();
        showtime.setHall(otherHall);

        given(showtimeRepository.findById(showtimeId)).willReturn(Optional.of(showtime));

        // When & Then
        assertThatThrownBy(() -> showtimeService.deleteShowtime(showtimeId, tenantAdmin))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("No access to this hall");

        verify(showtimeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteShowtime: Failure - should throw exception when showtime not found")
    void deleteShowtime_NotFound_ThrowsException() {
        // Given
        UUID fakeId = UUID.randomUUID();
        given(showtimeRepository.findById(fakeId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> showtimeService.deleteShowtime(fakeId, tenantAdmin))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("Showtime not found");

        verify(showtimeRepository, never()).delete(any());
    }

}
