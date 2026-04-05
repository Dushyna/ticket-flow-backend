package io.github.dushyna.ticketflow.cinema.controller.api;

import io.github.dushyna.ticketflow.cinema.dto.request.MovieCreateDto;
import io.github.dushyna.ticketflow.cinema.entity.Movie;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/v1/movies")
public interface MovieApi extends MovieApiSwaggerDoc {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Movie create(@Valid @RequestBody MovieCreateDto dto);

    @PutMapping("/{id}")
    Movie update(@PathVariable UUID id, @RequestBody MovieCreateDto dto);

    @GetMapping
    List<Movie> getAll();

    @GetMapping("/{id}")
    Movie getById(@PathVariable UUID id);

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id);

}
