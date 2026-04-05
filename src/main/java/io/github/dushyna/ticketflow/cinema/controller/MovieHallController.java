package io.github.dushyna.ticketflow.cinema.controller;

import io.github.dushyna.ticketflow.cinema.controller.api.MovieHallApi;
import io.github.dushyna.ticketflow.cinema.dto.request.MovieHallCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.MovieHallResponseDto;
import io.github.dushyna.ticketflow.cinema.service.interfaces.MovieHallService;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MovieHallController implements MovieHallApi {

    private final MovieHallService hallService;
    private final UserService userService;

    @Override
    public MovieHallResponseDto create(MovieHallCreateDto dto) {
        return hallService.createHall(dto, getCurrentUser());
    }

    @Override
    public List<MovieHallResponseDto> getAllByCinema(UUID cinemaId) {
        return hallService.getAllByCinema(cinemaId, getCurrentUser());
    }

    @Override
    public MovieHallResponseDto getById(UUID id) {
        return hallService.getByIdOrThrow(id, getCurrentUser());
    }

    @Override
    public MovieHallResponseDto update(UUID id, MovieHallCreateDto dto) {
        return hallService.updateHall(id, dto, getCurrentUser());
    }

    @Override
    public void delete(UUID id) {
        hallService.deleteHall(id, getCurrentUser());
    }

    private AppUser getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getByEmailOrThrow(email);
    }
}
