package io.github.dushyna.ticketflow.cinema.service.impl;

import io.github.dushyna.ticketflow.cinema.dto.request.MovieHallCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.MovieHallResponseDto;
import io.github.dushyna.ticketflow.cinema.entity.Cinema;
import io.github.dushyna.ticketflow.cinema.entity.MovieHall;
import io.github.dushyna.ticketflow.cinema.repository.CinemaRepository;
import io.github.dushyna.ticketflow.cinema.repository.MovieHallRepository;
import io.github.dushyna.ticketflow.cinema.service.interfaces.MovieHallService;
import io.github.dushyna.ticketflow.cinema.utils.MovieHallMapper;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service implementation for managing Movie Hall layouts.
 * Now correctly receives currentUser from the controller layer.
 */
@Service
@RequiredArgsConstructor
public class MovieHallServiceImpl implements MovieHallService {

    private final MovieHallRepository movieHallRepository;
    private final CinemaRepository cinemaRepository;
    private final MovieHallMapper mappingService;

    @Override
    @Transactional
    public MovieHallResponseDto createHall(MovieHallCreateDto dto, AppUser currentUser) {
        Cinema cinema = cinemaRepository.findById(dto.cinemaId())
                .orElseThrow(() -> new RuntimeException("Cinema not found with id: " + dto.cinemaId()));

        validateAccess(cinema, currentUser);

        MovieHall hall = new MovieHall();
        hall.setName(dto.name());
        hall.setCinema(cinema);
        hall.setRowsCount(dto.rows());
        hall.setColsCount(dto.cols());
        hall.setLayoutConfig(dto.layoutConfig());

        MovieHall saved = movieHallRepository.save(hall);
        return mappingService.mapEntityToResponseDto(saved);
    }

    @Override
    @Transactional
    public List<MovieHallResponseDto> getAllByCinema(UUID cinemaId, AppUser currentUser) {
        Cinema cinema = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new RuntimeException("Cinema not found with id: " + cinemaId));

        validateAccess(cinema, currentUser);

        return movieHallRepository.findAllByCinemaId(cinemaId).stream()
                .map(mappingService::mapEntityToResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public MovieHallResponseDto getByIdOrThrow(UUID id, AppUser currentUser) {
        MovieHall hall = movieHallRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hall not found with id: " + id));

        validateAccess(hall.getCinema(), currentUser);

        return mappingService.mapEntityToResponseDto(hall);
    }

    @Override
    @Transactional
    public void deleteHall(UUID id, AppUser currentUser) {
        MovieHall hall = movieHallRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hall not found with id: " + id));

        validateAccess(hall.getCinema(), currentUser);

        movieHallRepository.delete(hall);
    }

    @Override
    @Transactional
    public MovieHallResponseDto updateHall(UUID id, MovieHallCreateDto dto, AppUser currentUser) {
        MovieHall hall = movieHallRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hall not found with id: " + id));

        validateAccess(hall.getCinema(), currentUser);

        hall.setName(dto.name());
        hall.setRowsCount(dto.rows());
        hall.setColsCount(dto.cols());
        hall.setLayoutConfig(dto.layoutConfig());

        MovieHall saved = movieHallRepository.save(hall);
        return mappingService.mapEntityToResponseDto(saved);
    }

    private void validateAccess(Cinema cinema, AppUser user) {
        if (user.getOrganization() == null ||
                !cinema.getOrganization().getId().equals(user.getOrganization().getId())) {
            throw new AccessDeniedException("Forbidden: You don't have access to this cinema's halls");
        }
    }
}
