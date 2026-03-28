package io.github.dushyna.ticketflow.cinema.controller;

import io.github.dushyna.ticketflow.cinema.controller.api.CinemaApi;
import io.github.dushyna.ticketflow.cinema.dto.request.CinemaCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.CinemaResponseDto;
import io.github.dushyna.ticketflow.cinema.service.interfaces.CinemaService;
import io.github.dushyna.ticketflow.user.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
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
        String email = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();

        var currentUser = userService.getByEmailOrThrow(email);
        return cinemaService.createCinema(dto, currentUser);
    }

    @Override
    public List<CinemaResponseDto> getAll() {
        String email = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();

        var currentUser = userService.getByEmailOrThrow(email);
        return cinemaService.getAllByOrganization(currentUser);
    }

    @Override
    public CinemaResponseDto getById(UUID id) {
        String email = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();

        var currentUser = userService.getByEmailOrThrow(email);
        return cinemaService.getByIdOrThrow(id, currentUser);
    }
}
