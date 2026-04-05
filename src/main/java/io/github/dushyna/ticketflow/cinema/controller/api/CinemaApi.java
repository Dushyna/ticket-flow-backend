package io.github.dushyna.ticketflow.cinema.controller.api;

import io.github.dushyna.ticketflow.cinema.dto.request.CinemaCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.CinemaResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/v1/cinemas")
public interface CinemaApi extends CinemaApiSwaggerDoc {

    @PostMapping
    CinemaResponseDto create(@Valid @RequestBody CinemaCreateDto dto);

    @PatchMapping("/{id}")
    CinemaResponseDto update(@PathVariable UUID id, @Valid @RequestBody CinemaCreateDto dto);


    @GetMapping
    List<CinemaResponseDto> getAll();

    @GetMapping("/{id}")
    CinemaResponseDto getById(@PathVariable UUID id);

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id);

}
