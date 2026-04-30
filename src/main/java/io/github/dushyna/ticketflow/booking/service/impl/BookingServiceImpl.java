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
import io.github.dushyna.ticketflow.common.service.TranslationService;
import io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
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
    private final BookingMapper bookingMapper;
    private final ObjectMapper objectMapper;
    private final TranslationService translationService;

    private static final List<BookingStatus> ACTIVE_STATUSES =
            Arrays.asList(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBookings(BookingCreateDto dto, AppUser currentUser) {
        Showtime showtime = showtimeRepository.findById(dto.showtimeId())
                .orElseThrow(() -> new RestApiException(
                        HttpStatus.NOT_FOUND,
                        translationService.get("showtime.not_found")
                ));


        if (showtime.getStartTime().isBefore(Instant.now())) {
            throw new RestApiException(
                    HttpStatus.BAD_REQUEST,
                    translationService.get("booking.showtime.past")
            );
        }
       // 1. FIRST STEP: Validate all seats in the batch
        for (var seatDto : dto.seats()) {
            if (bookingRepository.existsByShowtimeIdAndRowIndexAndColIndexAndStatusIn(
                    showtime.getId(), seatDto.row(), seatDto.col(), ACTIVE_STATUSES)) {

                String errorMessage = translationService.get(
                        "booking.seat.taken",
                        seatDto.row() + 1,
                        seatDto.col() + 1
                );

                throw new RestApiException(HttpStatus.CONFLICT, errorMessage);
            }
        }

        // 2. SECOND STEP: If all are free, then save them

        MovieHall hall = showtime.getHall();
        Map<String, Object> layout = hall.getLayoutConfig();

        List<List<String>> grid = objectMapper.convertValue(layout.get("grid"), new TypeReference<>() {});
        List<Map<String, Object>> zoneConfigs = objectMapper.convertValue(layout.get("zoneConfigs"), new TypeReference<>() {});

        for (var seatDto : dto.seats()) {

            String seatZoneId = grid.get(seatDto.row()).get(seatDto.col());
            BigDecimal zoneMultiplier = zoneConfigs.stream()
                    .filter(config -> config.get("id").equals(seatZoneId))
                    .map(config -> new BigDecimal(config.getOrDefault("multiplier", "1").toString()))
                    .findFirst()
                    .orElse(BigDecimal.ONE);

            Booking booking = new Booking();
            booking.setUser(currentUser);
            booking.setShowtime(showtime);
            booking.setHall(hall);
            booking.setRowIndex(seatDto.row());
            booking.setColIndex(seatDto.col());
            booking.setStatus(BookingStatus.PENDING);

            BigDecimal ticketMultiplier = BigDecimal.ONE;
            if (seatDto.ticketTypeId() != null) {
                TicketType tt = ticketTypeRepository.findById(seatDto.ticketTypeId())
                        .orElseThrow(() -> new RestApiException(
                                HttpStatus.NOT_FOUND,
                                translationService.get("ticket_type.not_found")
                        ));
                booking.setTicketType(tt);
                ticketMultiplier = tt.getDiscount();
            }

            BigDecimal finalPrice = showtime.getBasePrice()
                    .multiply(zoneMultiplier)
                    .multiply(ticketMultiplier);

            booking.setFinalPrice(finalPrice);
            bookingRepository.save(booking);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getUserBookings(AppUser user) {
        return bookingRepository.findAllByUserIdWithDetails(user.getId())
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
