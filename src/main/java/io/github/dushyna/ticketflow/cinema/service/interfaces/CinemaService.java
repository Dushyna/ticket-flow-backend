package io.github.dushyna.ticketflow.cinema.service.interfaces;

import io.github.dushyna.ticketflow.cinema.dto.request.CinemaCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.CinemaResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing Cinema buildings/locations
 */
public interface CinemaService {

    CinemaResponseDto createCinema(CinemaCreateDto dto, AppUser currentUser);

    CinemaResponseDto updateCinema(UUID id, CinemaCreateDto dto, AppUser currentUser);


    List<CinemaResponseDto> getAllForUser(AppUser currentUser);

    CinemaResponseDto getByIdOrThrow(UUID id, AppUser currentUser);

    void deleteCinema(UUID id, AppUser currentUser);

}
