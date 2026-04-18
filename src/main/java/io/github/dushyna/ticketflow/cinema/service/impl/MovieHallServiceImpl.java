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
import io.github.dushyna.ticketflow.user.entity.Role;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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
                .orElseThrow(() -> new EntityNotFoundException("Cinema not found"));

        validateAccess(cinema, currentUser);

        MovieHall hall = mappingService.mapDtoToEntity(dto);
        hall.setCinema(cinema);

        return mappingService.mapEntityToResponseDto(movieHallRepository.save(hall));
    }

    @Override
    @Transactional
    public MovieHallResponseDto updateHall(UUID id, MovieHallCreateDto dto, AppUser currentUser) {
        MovieHall hall = findByIdOrThrow(id);
        validateAccess(hall.getCinema(), currentUser);

        mappingService.updateEntityFromDto(dto, hall);
        return mappingService.mapEntityToResponseDto(movieHallRepository.save(hall));
    }

    @Override
    @Transactional
    public List<MovieHallResponseDto> getAllByCinema(UUID cinemaId, AppUser currentUser) {
        if (currentUser != null && currentUser.getRole() == Role.ROLE_TENANT_ADMIN) {
            Cinema cinema = cinemaRepository.findById(cinemaId)
                    .orElseThrow(() -> new EntityNotFoundException("Cinema not found"));
            validateAccess(cinema, currentUser);
        }

        return movieHallRepository.findAllByCinemaId(cinemaId).stream()
                .map(mappingService::mapEntityToResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public MovieHallResponseDto getByIdOrThrow(UUID id, AppUser currentUser) {
        MovieHall hall = findByIdOrThrow(id);

        if (currentUser != null && currentUser.getRole() == Role.ROLE_TENANT_ADMIN) {
            validateAccess(hall.getCinema(), currentUser);
        }

        return mappingService.mapEntityToResponseDto(hall);
    }

    @Override
    @Transactional
    public void deleteHall(UUID id, AppUser currentUser) {
        MovieHall hall = findByIdOrThrow(id);
        validateAccess(hall.getCinema(), currentUser);
        movieHallRepository.delete(hall);
    }

    private MovieHall findByIdOrThrow(UUID id) {
        return movieHallRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hall not found with id: " + id));
    }

    private void validateAccess(Cinema cinema, AppUser user) {
        if (user.getRole() == io.github.dushyna.ticketflow.user.entity.Role.ROLE_SUPER_ADMIN) return;

        if (user.getOrganization() == null ||
                !cinema.getOrganization().getId().equals(user.getOrganization().getId())) {
            throw new AccessDeniedException("Forbidden: You don't have access to this cinema's halls");
        }
    }
}
