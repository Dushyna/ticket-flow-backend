package io.github.dushyna.ticketflow.booking.controller.api;

import io.github.dushyna.ticketflow.booking.dto.request.BookingCreateDto;
import io.github.dushyna.ticketflow.booking.dto.response.SeatCoordinateDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/v1/bookings")
public interface BookingApi extends BookingApiSwaggerDoc {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    void create(@Valid @RequestBody BookingCreateDto dto);

    @GetMapping("/hall/{hallId}/occupied")
    List<SeatCoordinateDto> getOccupied(@PathVariable UUID hallId);
}
