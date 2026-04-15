package io.github.dushyna.ticketflow.cinema.service.interfaces;

import io.github.dushyna.ticketflow.cinema.dto.request.ShowtimeCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.ShowtimeResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;

import java.util.List;
import java.util.UUID;

public interface ShowtimeService {

    ShowtimeResponseDto createShowtime(ShowtimeCreateDto dto, AppUser currentUser);

    ShowtimeResponseDto updateShowtime(UUID id, ShowtimeCreateDto dto, AppUser currentUser);

    void deleteShowtime(UUID id, AppUser currentUser);

    List<ShowtimeResponseDto> getShowtimesByHall(UUID hallId);

    List<ShowtimeResponseDto> getShowtimesByMovie(UUID movieId);

    List<ShowtimeResponseDto> getShowtimesByCinema(UUID cinemaId);

    ShowtimeResponseDto getByIdOrThrow(UUID id);
}
