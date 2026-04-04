package io.github.dushyna.ticketflow.booking.controller;

import io.github.dushyna.ticketflow.booking.controller.api.BookingApi;
import io.github.dushyna.ticketflow.booking.dto.request.BookingCreateDto;
import io.github.dushyna.ticketflow.booking.dto.response.SeatCoordinateDto;
import io.github.dushyna.ticketflow.booking.service.interfaces.BookingService;
import io.github.dushyna.ticketflow.user.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BookingController implements BookingApi {

    private final BookingService bookingService;
    private final UserService userService;

    @Override
    public void create(BookingCreateDto dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var currentUser = userService.getByEmailOrThrow(email);
        bookingService.createBookings(dto, currentUser);
    }

    @Override
    public List<SeatCoordinateDto> getOccupied(UUID hallId) {
        return bookingService.getOccupiedSeats(hallId);
    }
}
