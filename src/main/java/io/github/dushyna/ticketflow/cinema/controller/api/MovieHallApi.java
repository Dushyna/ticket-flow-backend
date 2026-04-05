package io.github.dushyna.ticketflow.cinema.controller.api;

import io.github.dushyna.ticketflow.cinema.dto.request.MovieHallCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.MovieHallResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/v1/halls")
public interface MovieHallApi extends MovieHallApiSwaggerDoc {

    @PostMapping
    MovieHallResponseDto create(
            @Valid @RequestBody MovieHallCreateDto dto
    );

    @GetMapping("/cinema/{cinemaId}")
    List<MovieHallResponseDto> getAllByCinema(
            @PathVariable UUID cinemaId
    );

    @GetMapping("/{id}")
    MovieHallResponseDto getById(
            @PathVariable UUID id
    );

    @PatchMapping("/{id}")
    MovieHallResponseDto update(
            @PathVariable UUID id,
            @Valid @RequestBody MovieHallCreateDto dto
    );

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id);

}
