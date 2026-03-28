package io.github.dushyna.ticketflow.cinema.service.interfaces;

import io.github.dushyna.ticketflow.cinema.dto.request.MovieHallCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.MovieHallResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing Movie Hall layouts and configurations
 */
public interface MovieHallService {

    MovieHallResponseDto createHall(MovieHallCreateDto dto, AppUser currentUser);

    MovieHallResponseDto getByIdOrThrow(UUID id, AppUser currentUser);

    List<MovieHallResponseDto> getAllByCinema(UUID cinemaId, AppUser currentUser);

    void deleteHall(UUID id, AppUser currentUser);
}
