package io.github.dushyna.ticketflow.cinema.controller;

import io.github.dushyna.ticketflow.cinema.controller.api.MovieHallApi;
import io.github.dushyna.ticketflow.cinema.dto.request.MovieHallCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.MovieHallResponseDto;
import io.github.dushyna.ticketflow.cinema.service.interfaces.MovieHallService;
import io.github.dushyna.ticketflow.security.dto.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MovieHallController implements MovieHallApi {

    private final MovieHallService hallService;

    @Override
    public MovieHallResponseDto create(MovieHallCreateDto dto,
                                       @AuthenticationPrincipal AuthUserDetails userDetails) {
        return hallService.createHall(dto, userDetails.user());
    }

    @Override
    public List<MovieHallResponseDto> getAllByCinema(UUID cinemaId,
                                                     @AuthenticationPrincipal AuthUserDetails userDetails) {
        return hallService.getAllByCinema(cinemaId, userDetails != null ? userDetails.user() : null);
    }

    @Override
    public MovieHallResponseDto getById(UUID id,
                                        @AuthenticationPrincipal AuthUserDetails userDetails) {
        return hallService.getByIdOrThrow(id, userDetails != null ? userDetails.user() : null);
    }

    @Override
    public MovieHallResponseDto update(UUID id, MovieHallCreateDto dto,
                                       @AuthenticationPrincipal AuthUserDetails userDetails) {
        return hallService.updateHall(id, dto, userDetails.user());
    }

    @Override
    public void delete(UUID id, @AuthenticationPrincipal AuthUserDetails userDetails) {
        hallService.deleteHall(id, userDetails.user());
    }
}
