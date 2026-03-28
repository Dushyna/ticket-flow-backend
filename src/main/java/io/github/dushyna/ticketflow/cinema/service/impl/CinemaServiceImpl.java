package io.github.dushyna.ticketflow.cinema.service.impl;

import io.github.dushyna.ticketflow.cinema.dto.request.CinemaCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.CinemaResponseDto;
import io.github.dushyna.ticketflow.cinema.entity.Cinema;
import io.github.dushyna.ticketflow.cinema.repository.CinemaRepository;
import io.github.dushyna.ticketflow.cinema.service.interfaces.CinemaService;
import io.github.dushyna.ticketflow.cinema.utils.CinemaMapper;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

        Cinema cinema = new Cinema();
        cinema.setName(dto.name());
        cinema.setAddress(dto.address());
        cinema.setOrganization(currentUser.getOrganization());

        Cinema saved = cinemaRepository.save(cinema);
        return mappingService.mapEntityToResponseDto(saved);
    }

    @Override
    @Transactional
    public List<CinemaResponseDto> getAllByOrganization(AppUser currentUser) {
        if (currentUser.getOrganization() == null) {
            return List.of();
        }

        return cinemaRepository.findAllByOrganizationId(currentUser.getOrganization().getId())
                .stream()
                .map(mappingService::mapEntityToResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public CinemaResponseDto getByIdOrThrow(UUID id, AppUser currentUser) {
        Cinema cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cinema not found with id: " + id));

        validateAccess(cinema, currentUser);

        return mappingService.mapEntityToResponseDto(cinema);
    }

    private void validateAccess(Cinema cinema, AppUser user) {
        if (user.getOrganization() == null ||
                !cinema.getOrganization().getId().equals(user.getOrganization().getId())) {
            throw new AccessDeniedException("Forbidden: You don't have access to this cinema");
        }
    }
}
