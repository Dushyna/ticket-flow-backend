package io.github.dushyna.ticketflow.cinema.controller;

import io.github.dushyna.ticketflow.cinema.controller.api.CinemaApi;
import io.github.dushyna.ticketflow.cinema.dto.request.CinemaCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.CinemaResponseDto;
import io.github.dushyna.ticketflow.cinema.service.interfaces.CinemaService;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CinemaController implements CinemaApi {

    private final CinemaService cinemaService;
    private final UserService userService;

    @Override
    public CinemaResponseDto create(CinemaCreateDto dto) {
        return cinemaService.createCinema(dto, getCurrentUser());
    }

    @Override
    public CinemaResponseDto update(UUID id, CinemaCreateDto dto) {
        return cinemaService.updateCinema(id, dto, getCurrentUser());
    }

    @Override
    public List<CinemaResponseDto> getAll() {
        return cinemaService.getAllByOrganization(getCurrentUser());
    }

    @Override
    public CinemaResponseDto getById(UUID id) {
        return cinemaService.getByIdOrThrow(id, getCurrentUser());
    }

    @Override
    public void delete(UUID id) {
        cinemaService.deleteCinema(id, getCurrentUser());
    }

    private AppUser getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getByEmailOrThrow(email);
    }

}
