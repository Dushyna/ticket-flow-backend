package io.github.dushyna.ticketflow.booking.controller.api;

import io.github.dushyna.ticketflow.booking.dto.request.BookingCreateDto;
import io.github.dushyna.ticketflow.booking.dto.response.SeatCoordinateDto;
import io.github.dushyna.ticketflow.booking.dto.response.BookingResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/v1/bookings")
public interface BookingApi extends BookingApiSwaggerDoc {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    void create(@Valid @RequestBody BookingCreateDto dto, @AuthenticationPrincipal AppUser user);

    @GetMapping("/occupied/{showtimeId}")
    List<SeatCoordinateDto> getOccupied(@PathVariable UUID showtimeId);

    @GetMapping("/my")
    List<BookingResponseDto> getMyBookings(@AuthenticationPrincipal AppUser user);

}
