package io.github.dushyna.ticketflow.booking.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dushyna.ticketflow.booking.dto.request.BookingCreateDto;
import io.github.dushyna.ticketflow.booking.dto.response.BookingResponseDto;
import io.github.dushyna.ticketflow.booking.dto.response.SeatCoordinateDto;
import io.github.dushyna.ticketflow.booking.entity.Booking;
import io.github.dushyna.ticketflow.booking.entity.BookingStatus;
import io.github.dushyna.ticketflow.booking.repository.BookingRepository;
import io.github.dushyna.ticketflow.booking.service.interfaces.BookingService;
import io.github.dushyna.ticketflow.booking.utils.BookingMapper;
import io.github.dushyna.ticketflow.cinema.entity.MovieHall;
import io.github.dushyna.ticketflow.cinema.entity.Showtime;
import io.github.dushyna.ticketflow.cinema.entity.TicketType;
import io.github.dushyna.ticketflow.cinema.repository.ShowtimeRepository;
import io.github.dushyna.ticketflow.cinema.repository.TicketTypeRepository;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ShowtimeRepository showtimeRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;
    private final ObjectMapper objectMapper;

    private static final List<BookingStatus> ACTIVE_STATUSES =
            Arrays.asList(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    @Override
    @Transactional
    public void createBookings(BookingCreateDto dto, AppUser currentUser) {
        String email = SecurityContextHolder
                .getContext().getAuthentication().getName();

        AppUser managedUser = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));

        Showtime showtime = showtimeRepository.findById(dto.showtimeId())
                .orElseThrow(() -> new EntityNotFoundException("Showtime not found"));

        MovieHall hall = showtime.getHall();
        Map<String, Object> layout = hall.getLayoutConfig();

        List<List<String>> grid = objectMapper.convertValue(
                layout.get("grid"),
                new TypeReference<>() {
                }
        );

        List<Map<String, Object>> zoneConfigs = objectMapper.convertValue(
                layout.get("zoneConfigs"),
                new TypeReference<>() {
                }
        );

        for (var seatDto : dto.seats()) {
            boolean isOccupied = bookingRepository.existsByShowtimeIdAndRowIndexAndColIndexAndStatusIn(
                    showtime.getId(), seatDto.row(), seatDto.col(), ACTIVE_STATUSES);

            if (isOccupied) {
                throw new RuntimeException("Seat at Row " + (seatDto.row() + 1) +
                        " Col " + (seatDto.col() + 1) + " is already taken");
            }

            String seatZoneId = grid.get(seatDto.row()).get(seatDto.col());
            BigDecimal zoneMultiplier = zoneConfigs.stream()
                    .filter(config -> config.get("id").equals(seatZoneId))
                    .map(config -> {
                        Object mult = config.get("multiplier");
                        return mult != null ? new BigDecimal(mult.toString()) : BigDecimal.ONE;
                    })
                    .findFirst()
                    .orElse(BigDecimal.ONE);

            Booking booking = new Booking();
            booking.setUser(managedUser);
            booking.setShowtime(showtime);
            booking.setHall(hall);
            booking.setRowIndex(seatDto.row());
            booking.setColIndex(seatDto.col());

            BigDecimal ticketMultiplier = BigDecimal.ONE;
            if (seatDto.ticketTypeId() != null) {
                TicketType tt = ticketTypeRepository.findById(seatDto.ticketTypeId())
                        .orElseThrow(() -> new EntityNotFoundException("Ticket type not found"));
                booking.setTicketType(tt);
                ticketMultiplier = tt.getDiscount();
            }

            BigDecimal finalPrice = showtime.getBasePrice()
                    .multiply(zoneMultiplier)
                    .multiply(ticketMultiplier);

            booking.setFinalPrice(finalPrice);
            booking.setStatus(BookingStatus.PENDING);
            bookingRepository.save(booking);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getUserBookings(AppUser user) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        AppUser managedUser = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return bookingRepository.findAllByUserIdWithDetails(managedUser.getId())
                .stream()
                .map(bookingMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatCoordinateDto> getOccupiedSeats(UUID showtimeId) {
        return bookingRepository.findAllByShowtimeIdAndStatusIn(showtimeId, ACTIVE_STATUSES).stream()
                .map(b -> new SeatCoordinateDto(b.getRowIndex(), b.getColIndex()))
                .toList();
    }
}
