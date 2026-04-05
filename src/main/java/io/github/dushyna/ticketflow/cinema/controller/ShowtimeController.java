package io.github.dushyna.ticketflow.cinema.controller;

import io.github.dushyna.ticketflow.cinema.controller.api.ShowtimeApi;
import io.github.dushyna.ticketflow.cinema.dto.request.ShowtimeCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.ShowtimeResponseDto;
import io.github.dushyna.ticketflow.cinema.service.interfaces.ShowtimeService;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ShowtimeController implements ShowtimeApi {

    private final ShowtimeService showtimeService;
    private final UserService userService;

    @Override
    public ShowtimeResponseDto create(ShowtimeCreateDto dto) {
        return showtimeService.createShowtime(dto, getCurrentUser());
    }

    @Override
    public ShowtimeResponseDto update(UUID id, ShowtimeCreateDto dto) {
        return showtimeService.updateShowtime(id, dto, getCurrentUser());
    }

    @Override
    public void delete(UUID id) {
        showtimeService.deleteShowtime(id, getCurrentUser());
    }

    @Override
    public ShowtimeResponseDto getById(UUID id) {
        return showtimeService.getByIdOrThrow(id);
    }

    @Override
    public List<ShowtimeResponseDto> getByMovie(UUID movieId) {
        return showtimeService.getShowtimesByMovie(movieId);
    }

    @Override
    public List<ShowtimeResponseDto> getByHall(UUID hallId) {
        return showtimeService.getShowtimesByHall(hallId);
    }

    private AppUser getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getByEmailOrThrow(email);
    }
}
