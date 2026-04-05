package io.github.dushyna.ticketflow.cinema.controller.api;

import io.github.dushyna.ticketflow.cinema.dto.request.ShowtimeCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.ShowtimeResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/v1/showtimes")
public interface ShowtimeApi extends ShowtimeApiSwaggerDoc {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ShowtimeResponseDto create(@Valid @RequestBody ShowtimeCreateDto dto);

    @PatchMapping("/{id}")
    ShowtimeResponseDto update(@PathVariable UUID id, @Valid @RequestBody ShowtimeCreateDto dto);

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id);

    @GetMapping("/{id}")
    ShowtimeResponseDto getById(@PathVariable UUID id);

    @GetMapping("/movie/{movieId}")
    List<ShowtimeResponseDto> getByMovie(@PathVariable UUID movieId);

    @GetMapping("/hall/{hallId}")
    List<ShowtimeResponseDto> getByHall(@PathVariable UUID hallId);
}
