package io.github.dushyna.ticketflow.cinema.controller;

import io.github.dushyna.ticketflow.cinema.controller.api.CinemaApi;
import io.github.dushyna.ticketflow.cinema.dto.request.CinemaCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.CinemaResponseDto;
import io.github.dushyna.ticketflow.cinema.service.interfaces.CinemaService;
import io.github.dushyna.ticketflow.security.dto.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CinemaController implements CinemaApi {

    private final CinemaService cinemaService;

    @Override
    public CinemaResponseDto create(CinemaCreateDto dto,
                                    @AuthenticationPrincipal AuthUserDetails userDetails) {
        return cinemaService.createCinema(dto, userDetails.user());
    }

    @Override
    public CinemaResponseDto update(UUID id, CinemaCreateDto dto,
                                    @AuthenticationPrincipal AuthUserDetails userDetails) {
        return cinemaService.updateCinema(id, dto, userDetails.user());
    }

    @Override
    public List<CinemaResponseDto> getAll(@AuthenticationPrincipal AuthUserDetails userDetails) {
        return cinemaService.getAllForUser(userDetails != null ? userDetails.user() : null);
    }

    @Override
    public CinemaResponseDto getById(UUID id,
                                     @AuthenticationPrincipal AuthUserDetails userDetails) {
        return cinemaService.getByIdOrThrow(id, userDetails != null ? userDetails.user() : null);
    }

    @Override
    public void delete(UUID id, @AuthenticationPrincipal AuthUserDetails userDetails) {
        cinemaService.deleteCinema(id, userDetails.user());
    }
}
