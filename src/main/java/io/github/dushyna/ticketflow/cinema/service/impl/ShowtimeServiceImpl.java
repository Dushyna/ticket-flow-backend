package io.github.dushyna.ticketflow.cinema.service.impl;

import io.github.dushyna.ticketflow.cinema.dto.request.ShowtimeCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.ShowtimeResponseDto;
import io.github.dushyna.ticketflow.cinema.entity.Movie;
import io.github.dushyna.ticketflow.cinema.entity.MovieHall;
import io.github.dushyna.ticketflow.cinema.entity.Showtime;
import io.github.dushyna.ticketflow.cinema.repository.MovieHallRepository;
import io.github.dushyna.ticketflow.cinema.repository.MovieRepository;
import io.github.dushyna.ticketflow.cinema.repository.ShowtimeRepository;
import io.github.dushyna.ticketflow.cinema.service.interfaces.ShowtimeService;
import io.github.dushyna.ticketflow.cinema.utils.ShowtimeMapper;
import io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShowtimeServiceImpl implements ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final MovieHallRepository hallRepository;
    private final ShowtimeMapper showtimeMapper;

    private static final int CLEANING_BUFFER_MINUTES = 15;

    @Override
    @Transactional
    public ShowtimeResponseDto createShowtime(ShowtimeCreateDto dto, AppUser currentUser) {
        Movie movie = movieRepository.findById(dto.movieId())
                .orElseThrow(() -> new EntityNotFoundException("Movie not found"));

        MovieHall hall = hallRepository.findById(dto.hallId())
                .orElseThrow(() -> new EntityNotFoundException("Hall not found"));

        validateHallAccess(hall, currentUser);

        Instant startTime = dto.startTime();

        if (startTime.isBefore(Instant.now())) {
            throw new RestApiException(HttpStatus.BAD_REQUEST, "Cannot schedule a showtime in the past");
        }

        Instant endTime = calculateEndTime(startTime, movie.getDurationMinutes());

        validateNoConflicts(hall.getId(), startTime, endTime, null);

        Showtime showtime = showtimeMapper.mapCreateDtoToEntity(dto);
        showtime.setMovie(movie);
        showtime.setHall(hall);
        showtime.setEndTime(endTime);

        return showtimeMapper.mapEntityToResponseDto(showtimeRepository.save(showtime));
    }

    @Override
    @Transactional
    public ShowtimeResponseDto updateShowtime(UUID id, ShowtimeCreateDto dto, AppUser currentUser) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Showtime not found"));

        validateHallAccess(showtime.getHall(), currentUser);

        showtimeMapper.updateEntityFromDto(dto, showtime);

        if (dto.movieId() != null) {
            Movie movie = movieRepository.findById(dto.movieId())
                    .orElseThrow(() -> new EntityNotFoundException("Movie not found"));
            showtime.setMovie(movie);
        }

        showtime.setEndTime(calculateEndTime(showtime.getStartTime(), showtime.getMovie().getDurationMinutes()));

        validateNoConflicts(showtime.getHall().getId(), showtime.getStartTime(), showtime.getEndTime(), id);

        return showtimeMapper.mapEntityToResponseDto(showtimeRepository.save(showtime));
    }

    @Override
    @Transactional
    public void deleteShowtime(UUID id, AppUser currentUser) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Showtime not found"));

        validateHallAccess(showtime.getHall(), currentUser);
        showtimeRepository.delete(showtime);
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeResponseDto getByIdOrThrow(UUID id) {
        return showtimeRepository.findById(id)
                .map(showtimeMapper::mapEntityToResponseDto)
                .orElseThrow(() -> new EntityNotFoundException("Showtime not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponseDto> getShowtimesByHall(UUID hallId) {
        return showtimeRepository.findAllByHallId(hallId).stream()
                .map(showtimeMapper::mapEntityToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponseDto> getShowtimesByMovie(UUID movieId) {
        return showtimeRepository.findAllByMovieId(movieId).stream()
                .map(showtimeMapper::mapEntityToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponseDto> getShowtimesByCinema(UUID cinemaId) {
        List<Showtime> showtimes = showtimeRepository.findAllByCinemaIdWithDetails(cinemaId);
        System.out.println("DEBUG: Found " + showtimes.size() + " showtimes for cinema " + cinemaId);
        return showtimes.stream()
                .map(showtimeMapper::mapEntityToResponseDto)
                .toList();
    }

    private void validateNoConflicts(UUID hallId, Instant start, Instant end, UUID excludeId) {
        boolean hasConflict = showtimeRepository.existsOverlappingShowtime(hallId, start, end, excludeId);
        if (hasConflict) {
            throw new RestApiException(
                    HttpStatus.CONFLICT,
                    "Conflict: The hall is already booked for another showtime."
            );
        }
    }

    private Instant calculateEndTime(Instant startTime, Integer durationMinutes) {
        return startTime.plus(Duration.ofMinutes(durationMinutes + CLEANING_BUFFER_MINUTES));
    }


    private void validateHallAccess(MovieHall hall, AppUser user) {
        if (user.getRole() == io.github.dushyna.ticketflow.user.entity.Role.ROLE_SUPER_ADMIN) return;

        if (user.getOrganization() == null ||
                !hall.getCinema().getOrganization().getId().equals(user.getOrganization().getId())) {
            throw new org.springframework.security.access.AccessDeniedException("No access to this hall");
        }
    }

}
