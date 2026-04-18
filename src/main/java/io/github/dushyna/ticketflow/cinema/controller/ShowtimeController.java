package io.github.dushyna.ticketflow.cinema.controller;

import io.github.dushyna.ticketflow.cinema.controller.api.ShowtimeApi;
import io.github.dushyna.ticketflow.cinema.dto.request.ShowtimeCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.ShowtimeResponseDto;
import io.github.dushyna.ticketflow.cinema.service.interfaces.ShowtimeService;
import io.github.dushyna.ticketflow.security.dto.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ShowtimeController implements ShowtimeApi {

    private final ShowtimeService showtimeService;

    @Override
    public ShowtimeResponseDto create(ShowtimeCreateDto dto,
                                      @AuthenticationPrincipal AuthUserDetails userDetails) {
        return showtimeService.createShowtime(dto, userDetails.user());
    }

    @Override
    public ShowtimeResponseDto update(UUID id, ShowtimeCreateDto dto,
                                      @AuthenticationPrincipal AuthUserDetails userDetails) {
        return showtimeService.updateShowtime(id, dto, userDetails.user());
    }

    @Override
    public void delete(UUID id, @AuthenticationPrincipal AuthUserDetails userDetails) {
        showtimeService.deleteShowtime(id, userDetails.user());
    }

    @Override
    public ShowtimeResponseDto getById(UUID id,
                                       @AuthenticationPrincipal AuthUserDetails userDetails) {
        // userDetails може бути null (для публічного доступу)
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

    @Override
    public List<ShowtimeResponseDto> getByCinema(UUID cinemaId) {
        return showtimeService.getShowtimesByCinema(cinemaId);
    }
}
