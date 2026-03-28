package io.github.dushyna.ticketflow.cinema.controller;

import io.github.dushyna.ticketflow.cinema.controller.api.MovieHallApi;
import io.github.dushyna.ticketflow.cinema.dto.request.MovieHallCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.MovieHallResponseDto;
import io.github.dushyna.ticketflow.cinema.service.interfaces.MovieHallService;
import io.github.dushyna.ticketflow.user.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder; // Додано
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
        // Отримуємо email з контексту безпеки
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var currentUser = userService.getByEmailOrThrow(email);
        return hallService.createHall(dto, currentUser);
    }

    @Override
    public List<MovieHallResponseDto> getAllByCinema(UUID cinemaId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var currentUser = userService.getByEmailOrThrow(email);
        return hallService.getAllByCinema(cinemaId, currentUser);
    }

    @Override
    public MovieHallResponseDto getById(UUID id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var currentUser = userService.getByEmailOrThrow(email);
        return hallService.getByIdOrThrow(id, currentUser);
    }
}
