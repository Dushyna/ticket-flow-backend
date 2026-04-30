package io.github.dushyna.ticketflow.booking.service.impl;

import io.github.dushyna.ticketflow.booking.dto.request.BookingCreateDto;
import io.github.dushyna.ticketflow.booking.dto.request.SeatCoordinateRequestDto;
import io.github.dushyna.ticketflow.booking.entity.Booking;
import io.github.dushyna.ticketflow.booking.repository.BookingRepository;
import io.github.dushyna.ticketflow.cinema.entity.*;
import io.github.dushyna.ticketflow.cinema.repository.*;
import io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException;
import io.github.dushyna.ticketflow.organization.entity.Organization;
import io.github.dushyna.ticketflow.organization.repository.OrganizationRepository;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.web.locale=en",
                "EMAIL_USERNAME=test@test.com",
                "EMAIL_PASSWORD=test-password",
                "EMAIL_HOST=localhost",
                "CLIENT_ID=test-id",
                "CLIENT_SECRET=test-secret",
                "JWT_AT_SECRET=dGhpc2lzYXZlcnlzZWN1cmVhY2Nlc3N0b2tlbnNlY3JldGtleTI1Ng==",
                "JWT_RT_SECRET=dGhpc2lzYXZlcnlzZWN1cmVpbmZ1c2lvbnJlZnJlc2h0b2tlbnNlY3JldGtleTI1Ng=="
        }
)
@ActiveProfiles("test")
@Transactional
class BookingIntegrationTest {

    @Autowired private BookingServiceImpl bookingService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private ShowtimeRepository showtimeRepository;
    @Autowired private MovieHallRepository hallRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private CinemaRepository cinemaRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TicketTypeRepository ticketTypeRepository;

    private AppUser user;
    private Showtime showtime;
    private MovieHall hall;
    private Movie movie;

    @BeforeEach
    void setUp() {
        Locale.setDefault(Locale.ENGLISH);
        Organization org = organizationRepository.save(Organization.builder().name("Booking Org").slug("booking-org").build());
        user = userRepository.save(new AppUser("pass", "test-buyer@test.com"));

        movie = new Movie();
        movie.setTitle("Inception");
        movie.setDurationMinutes(148);
        movie = movieRepository.save(movie);

        Cinema cinema = cinemaRepository.save(new Cinema("Integration Cinema", org));

        hall = new MovieHall();
        hall.setName("VIP Hall");
        hall.setCinema(cinema);
        hall.setRowsCount(5);
        hall.setColsCount(5);
        hall.setLayoutConfig(Map.of(
                "grid", List.of(List.of("vip-zone", "standard-zone")),
                "zoneConfigs", List.of(
                        Map.of("id", "vip-zone", "multiplier", 2.0),
                        Map.of("id", "standard-zone", "multiplier", 1.0)
                )
        ));
        hall = hallRepository.save(hall);

        showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setHall(hall);
        showtime.setStartTime(Instant.now().plusSeconds(3600));
        showtime.setEndTime(Instant.now().plusSeconds(7200));
        showtime.setBasePrice(new BigDecimal("100.00"));
        showtime = showtimeRepository.save(showtime);
    }

    @Test
    @DisplayName("Should correctly calculate prices based on JSON layout multipliers from DB")
    void shouldCalculatePriceFromDbJson() {
        // Given: Book one VIP seat (multiplier 2.0) and one standard seat (multiplier 1.0)
        SeatCoordinateRequestDto vipSeat = new SeatCoordinateRequestDto(0, 0, null);
        SeatCoordinateRequestDto stdSeat = new SeatCoordinateRequestDto(0, 1, null);
        BookingCreateDto dto = new BookingCreateDto(showtime.getId(), List.of(vipSeat, stdSeat));

        // When
        bookingService.createBookings(dto, user);

        // Then
        List<Booking> bookings = bookingRepository.findAll();
        assertThat(bookings).hasSize(2);

        Booking vipBooking = bookings.stream().filter(b -> b.getColIndex() == 0).findFirst().orElseThrow();
        Booking stdBooking = bookings.stream().filter(b -> b.getColIndex() == 1).findFirst().orElseThrow();

        // 100.00 * 2.0 = 200.00
        assertThat(vipBooking.getFinalPrice()).isEqualByComparingTo("200.00");
        // 100.00 * 1.0 = 100.00
        assertThat(stdBooking.getFinalPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Should throw CONFLICT when trying to book an already occupied seat")
    void shouldPreventDoubleBooking() {
        // Given: First booking for seat 0:0
        SeatCoordinateRequestDto seat = new SeatCoordinateRequestDto(0, 0, null);
        bookingService.createBookings(new BookingCreateDto(showtime.getId(), List.of(seat)), user);

        // When: Another attempt to book the SAME seat
        BookingCreateDto duplicateDto = new BookingCreateDto(showtime.getId(), List.of(seat));

        // Then
        assertThatThrownBy(() -> bookingService.createBookings(duplicateDto, user))
                .isInstanceOf(RestApiException.class)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.CONFLICT)
                .hasMessageContaining("is already taken");
    }

    @Test
    @DisplayName("Edge Case: Transactional rollback - should book NOTHING if one seat in a batch is taken")
    void shouldRollback_WhenOneSeatInBatchIsTaken() {
        // 1. Given: Seat 0:0 is already taken by someone else
        SeatCoordinateRequestDto takenSeat = new SeatCoordinateRequestDto(0, 0, null);
        bookingService.createBookings(new BookingCreateDto(showtime.getId(), List.of(takenSeat)), user);
        long countBefore = bookingRepository.count();
        assertThat(countBefore).isEqualTo(1L);

        // 2. When: Attempt to book two seats: 0:1 (free) AND 0:0 (taken)
        SeatCoordinateRequestDto freeSeat = new SeatCoordinateRequestDto(0, 1, null);
        BookingCreateDto batchDto = new BookingCreateDto(showtime.getId(), List.of(freeSeat, takenSeat));

        // 3. Then: Should throw Conflict and NOT save the free seat
        assertThatThrownBy(() -> bookingService.createBookings(batchDto, user))
                .isInstanceOf(RestApiException.class);

        List<Booking> allBookings = bookingRepository.findAll();

        assertThat(allBookings).hasSize((int) countBefore);
        assertThat(allBookings).noneMatch(b -> b.getRowIndex() == 0 && b.getColIndex() == 1);
    }

    @Test
    @DisplayName("Edge Case: Fallback to multiplier 1.0 if zone is missing in JSON configs")
    void shouldFallbackToOne_WhenZoneNotFoundInConfig() {
        // Given: seat belongs to "ghost-zone" which is not in zoneConfigs
        hall.setLayoutConfig(Map.of(
                "grid", List.of(List.of("ghost-zone")),
                "zoneConfigs", List.of() // Empty configs
        ));
        hallRepository.saveAndFlush(hall);

        BookingCreateDto dto = new BookingCreateDto(showtime.getId(), List.of(new SeatCoordinateRequestDto(0, 0, null)));

        // When
        bookingService.createBookings(dto, user);

        // Then: Price should be equal to basePrice (100.00 * 1.0)
        Booking booking = bookingRepository.findAll().stream()
                .filter(b -> b.getUser().getId().equals(user.getId()))
                .findFirst().orElseThrow();
        assertThat(booking.getFinalPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Edge Case: Complex calculation - Zone Multiplier * Ticket Discount")
    void shouldCalculatePrice_WithZoneAndTicketType() {
        // 1. Given: Create a Student ticket type (50% discount)
        TicketType studentTicket = new TicketType();
        studentTicket.setLabel("Student");
        studentTicket.setDiscount(new BigDecimal("0.50"));
        studentTicket.setOrganization(organizationRepository.findAll().getFirst());
        studentTicket = ticketTypeRepository.save(studentTicket);

        // Seat 0:0 is VIP (multiplier 2.0 from setUp)
        BookingCreateDto dto = new BookingCreateDto(showtime.getId(),
                List.of(new SeatCoordinateRequestDto(0, 0, studentTicket.getId())));

        // 2. When
        bookingService.createBookings(dto, user);

        // 3. Then: Calculation: 100.00 (base) * 2.0 (VIP) * 0.5 (Student) = 100.00
        Booking booking = bookingRepository.findAll().stream()
                .filter(b -> b.getTicketType() != null).findFirst().orElseThrow();
        assertThat(booking.getFinalPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Edge Case: Should throw exception when trying to book a showtime in the past")
    void shouldThrowException_WhenBookingPastShowtime() {
        // 1. Given: Створюємо сеанс, який був годину тому
        Showtime pastShowtime = new Showtime();
        pastShowtime.setMovie(movie);
        pastShowtime.setHall(hall);
        pastShowtime.setStartTime(Instant.now().minusSeconds(7200));
        pastShowtime.setEndTime(Instant.now().minusSeconds(3600));
        pastShowtime.setBasePrice(new BigDecimal("100.00"));
        pastShowtime = showtimeRepository.save(pastShowtime);

        BookingCreateDto dto = new BookingCreateDto(pastShowtime.getId(),
                List.of(new SeatCoordinateRequestDto(0, 0, null)));

        // 2. When & Then
        assertThatThrownBy(() -> bookingService.createBookings(dto, user))
                .isInstanceOf(RestApiException.class)
                .satisfies(ex -> {
                    RestApiException apiEx = (RestApiException) ex;
                    assertThat(apiEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getMessage()).contains("past");
                });
    }

}
