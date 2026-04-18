package io.github.dushyna.ticketflow.cinema.service.impl;

import io.github.dushyna.ticketflow.cinema.dto.request.CinemaCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.CinemaResponseDto;
import io.github.dushyna.ticketflow.cinema.entity.Cinema;
import io.github.dushyna.ticketflow.cinema.repository.CinemaRepository;
import io.github.dushyna.ticketflow.cinema.service.interfaces.CinemaService;
import io.github.dushyna.ticketflow.cinema.utils.CinemaMapper;
import io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.entity.Role;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CinemaServiceImpl implements CinemaService {

    private final CinemaRepository cinemaRepository;
    private final CinemaMapper mappingService;

    @Override
    @Transactional
    public CinemaResponseDto createCinema(CinemaCreateDto dto, AppUser currentUser) {
        if (currentUser.getOrganization() == null) {
            throw new AccessDeniedException("User must belong to an organization to create a cinema");
        }

        Cinema cinema = mappingService.mapDtoToEntity(dto);
        cinema.setOrganization(currentUser.getOrganization());

        return mappingService.mapEntityToResponseDto(cinemaRepository.save(cinema));
    }

    @Override
    @Transactional
    public CinemaResponseDto updateCinema(UUID id, CinemaCreateDto dto, AppUser currentUser) {
        Cinema cinema = findByIdOrThrow(id);
        validateAccess(cinema, currentUser);

        mappingService.updateEntityFromDto(dto, cinema);
        return mappingService.mapEntityToResponseDto(cinemaRepository.save(cinema));
    }

    @Override
    @Transactional
    public List<CinemaResponseDto> getAllForUser(AppUser currentUser) {
        if (currentUser == null || currentUser.getRole() == Role.ROLE_USER) {
            return cinemaRepository.findAll().stream()
                    .map(mappingService::mapEntityToResponseDto)
                    .toList();
        }

        if (currentUser.getRole() == Role.ROLE_SUPER_ADMIN) {
            return cinemaRepository.findAll().stream()
                    .map(mappingService::mapEntityToResponseDto)
                    .toList();
        }

        return cinemaRepository.findAllByOrganizationId(currentUser.getOrganization().getId())
                .stream()
                .map(mappingService::mapEntityToResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public CinemaResponseDto getByIdOrThrow(UUID id, AppUser currentUser) {
        Cinema cinema = findByIdOrThrow(id);

        if (currentUser != null && currentUser.getRole() == Role.ROLE_TENANT_ADMIN) {
            validateAccess(cinema, currentUser);
        }

        return mappingService.mapEntityToResponseDto(cinema);
    }

    @Override
    @Transactional
    public void deleteCinema(UUID id, AppUser currentUser) {
        Cinema cinema = findByIdOrThrow(id);
        validateAccess(cinema, currentUser);
        cinemaRepository.delete(cinema);
    }

    private Cinema findByIdOrThrow(UUID id) {
        return cinemaRepository.findById(id)
                .orElseThrow(() -> new RestApiException(HttpStatus.NOT_FOUND, "Cinema not found"));
    }

    private void validateAccess(Cinema cinema, AppUser user) {
        if (user.getRole() == Role.ROLE_SUPER_ADMIN) return;

        if (user.getOrganization() == null ||
                !cinema.getOrganization().getId().equals(user.getOrganization().getId())) {
            throw new AccessDeniedException("Forbidden: Access denied to this cinema");
        }
    }
}
