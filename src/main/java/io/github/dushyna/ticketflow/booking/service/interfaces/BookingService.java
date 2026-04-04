package io.github.dushyna.ticketflow.booking.service.interfaces;

import io.github.dushyna.ticketflow.booking.dto.request.BookingCreateDto;
import io.github.dushyna.ticketflow.booking.dto.response.SeatCoordinateDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;

import java.util.List;
import java.util.UUID;

public interface BookingService {
    void createBookings(BookingCreateDto dto, AppUser user);

    List<SeatCoordinateDto> getOccupiedSeats(UUID hallId);
}
