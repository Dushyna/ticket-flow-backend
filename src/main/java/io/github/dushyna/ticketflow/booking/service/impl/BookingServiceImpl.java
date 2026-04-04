package io.github.dushyna.ticketflow.booking.service.impl;

import io.github.dushyna.ticketflow.booking.dto.request.BookingCreateDto;
import io.github.dushyna.ticketflow.booking.dto.response.SeatCoordinateDto;
import io.github.dushyna.ticketflow.booking.entity.Booking;
import io.github.dushyna.ticketflow.booking.entity.BookingStatus;
import io.github.dushyna.ticketflow.booking.repository.BookingRepository;
import io.github.dushyna.ticketflow.booking.service.interfaces.BookingService;
import io.github.dushyna.ticketflow.cinema.entity.MovieHall;
import io.github.dushyna.ticketflow.cinema.repository.MovieHallRepository;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final MovieHallRepository movieHallRepository;

    private static final List<BookingStatus> ACTIVE_STATUSES =
            Arrays.asList(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    @Override
    @Transactional
    public void createBookings(BookingCreateDto dto, AppUser user) {
        MovieHall hall = movieHallRepository.findById(dto.hallId())
                .orElseThrow(() -> new RuntimeException("Hall not found"));

        for (var seat : dto.seats()) {
            boolean isOccupied = bookingRepository.existsByHallIdAndRowIndexAndColIndexAndStatusIn(
                    hall.getId(), seat.row(), seat.col(), ACTIVE_STATUSES);

            if (isOccupied) {
                throw new RuntimeException("Seat at Row " + (seat.row() + 1) +
                        " Col " + (seat.col() + 1) + " is already taken");
            }

            Booking booking = new Booking();
            booking.setUser(user);
            booking.setHall(hall);
            booking.setRowIndex(seat.row());
            booking.setColIndex(seat.col());
            booking.setStatus(BookingStatus.PENDING);

            bookingRepository.save(booking);
        }
    }

    @Override
    public List<SeatCoordinateDto> getOccupiedSeats(UUID hallId) {
        return bookingRepository.findAllActiveBookings(hallId, ACTIVE_STATUSES).stream()
                .map(b -> new SeatCoordinateDto(b.getRowIndex(), b.getColIndex()))
                .toList();
    }
}
