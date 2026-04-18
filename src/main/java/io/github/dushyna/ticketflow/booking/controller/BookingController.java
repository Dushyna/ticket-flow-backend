package io.github.dushyna.ticketflow.booking.controller;

import io.github.dushyna.ticketflow.booking.controller.api.BookingApi;
import io.github.dushyna.ticketflow.booking.dto.request.BookingCreateDto;
import io.github.dushyna.ticketflow.booking.dto.response.BookingResponseDto;
import io.github.dushyna.ticketflow.booking.dto.response.SeatCoordinateDto;
import io.github.dushyna.ticketflow.booking.service.interfaces.BookingService;
import io.github.dushyna.ticketflow.security.dto.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BookingController implements BookingApi {

    private final BookingService bookingService;

    @Override
    public void create(BookingCreateDto dto,
                       @AuthenticationPrincipal AuthUserDetails userDetails) {
        bookingService.createBookings(dto, userDetails.user());
    }

    @Override
    public List<SeatCoordinateDto> getOccupied(UUID showtimeId) {
        return bookingService.getOccupiedSeats(showtimeId);
    }

    @Override
    public List<BookingResponseDto> getMyBookings(@AuthenticationPrincipal AuthUserDetails userDetails) {
        return bookingService.getUserBookings(userDetails.user());
    }
}
