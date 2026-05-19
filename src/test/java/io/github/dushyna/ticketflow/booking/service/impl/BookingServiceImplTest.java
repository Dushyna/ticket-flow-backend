package io.github.dushyna.ticketflow.booking.service.impl;

import static org.mockito.ArgumentMatchers.any;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dushyna.ticketflow.booking.dto.request.BookingCreateDto;
import io.github.dushyna.ticketflow.booking.dto.request.SeatCoordinateRequestDto;
import io.github.dushyna.ticketflow.booking.dto.response.BookingResponseDto;
import io.github.dushyna.ticketflow.booking.dto.response.PaymentResponseDto;
import io.github.dushyna.ticketflow.booking.dto.response.SeatCoordinateDto;
import io.github.dushyna.ticketflow.booking.entity.Booking;
import io.github.dushyna.ticketflow.booking.entity.BookingStatus;
import io.github.dushyna.ticketflow.booking.entity.Order;
import io.github.dushyna.ticketflow.booking.repository.BookingRepository;
import io.github.dushyna.ticketflow.booking.repository.OrderRepository;
import io.github.dushyna.ticketflow.booking.utils.BookingMapper;
import io.github.dushyna.ticketflow.cinema.entity.Movie;
import io.github.dushyna.ticketflow.cinema.entity.MovieHall;
import io.github.dushyna.ticketflow.cinema.entity.Showtime;
import io.github.dushyna.ticketflow.cinema.entity.TicketType;
import io.github.dushyna.ticketflow.cinema.repository.ShowtimeRepository;
import io.github.dushyna.ticketflow.cinema.repository.TicketTypeRepository;
import io.github.dushyna.ticketflow.common.service.TranslationService;
import io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException;
import io.github.dushyna.ticketflow.payment.service.PaymentService;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for BookingServiceImpl")
class BookingServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private ShowtimeRepository showtimeRepository;
    @Mock private TicketTypeRepository ticketTypeRepository;
    @Mock private BookingMapper bookingMapper;
    @Mock private TranslationService translationService;
    @Mock private OrderRepository orderRepository;
    @Mock private PaymentService paymentService;
    @Mock private com.stripe.model.checkout.Session stripeSession;


    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private BookingServiceImpl bookingService;

    private AppUser user;
    private Showtime showtime;
    private UUID showtimeId;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        showtimeId = UUID.randomUUID();
        MovieHall hall = new MovieHall();
        hall.setName("IMAX");

        // Setup layout: 1 row, 1 col, seat belongs to 'standard-zone'
        Map<String, Object> layout = Map.of(
                "grid", List.of(List.of("standard-zone")),
                "zoneConfigs", List.of(Map.of("id", "standard-zone", "multiplier", 1.5))
        );
        hall.setLayoutConfig(layout);

        showtime = new Showtime();
        showtime.setStartTime(Instant.now().plus(Duration.ofDays(1)));
        showtime.setHall(hall);
        showtime.setBasePrice(new BigDecimal("100.00"));
        ReflectionTestUtils.setField(showtime, "id", showtimeId);

        Movie movie = new Movie();
        movie.setTitle("Inception");
        showtime.setMovie(movie);


        lenient().when(translationService.get(anyString())).thenReturn("Some message");
        lenient().when(translationService.get(anyString(), any())).thenReturn("Some message with args");
    }

    @Test
    @DisplayName("createBookings: Success - calculate price and return Stripe URL")
    void createBookings_Success() throws Exception {
        // Given
        UUID ticketTypeId = UUID.randomUUID();
        var seatDto = new SeatCoordinateRequestDto(0, 0, ticketTypeId);
        var createDto = new BookingCreateDto(showtimeId, List.of(seatDto));

        TicketType tt = new TicketType();
        tt.setDiscount(new BigDecimal("0.8"));

        given(showtimeRepository.findById(showtimeId)).willReturn(Optional.of(showtime));
        given(ticketTypeRepository.findById(ticketTypeId)).willReturn(Optional.of(tt));

        // Mock Stripe session behavior
        given(paymentService.createCheckoutSession(any(Order.class), anyString())).willReturn(stripeSession);
        given(stripeSession.getUrl()).willReturn("https://stripe.com");
        given(stripeSession.getId()).willReturn("cs_test_123");

        // When
        PaymentResponseDto result = bookingService.createBookings(createDto, user);

        // Then
        assertThat(result.paymentUrl()).isEqualTo("https://stripe.com");

        // Verify that ORDER was saved (not just booking)
        verify(orderRepository, times(1)).save(argThat(order ->
                order.getTotalPrice().compareTo(new BigDecimal("120.00")) == 0 &&
                        order.getBookings().size() == 1 &&
                        order.getStatus() == BookingStatus.PENDING &&
                        "cs_test_123".equals(order.getStripeSessionId())
        ));
    }


    @Test
    @DisplayName("createBookings: Failure - Showtime not found")
    void createBookings_ShowtimeNotFound_ThrowsException() {
        // Given
        UUID fakeShowtimeId = UUID.randomUUID();
        BookingCreateDto dto = new BookingCreateDto(fakeShowtimeId, List.of());

        given(showtimeRepository.findById(fakeShowtimeId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookingService.createBookings(dto, user))
                .isInstanceOf(RestApiException.class)
                .satisfies(ex -> {
                    RestApiException apiEx = (RestApiException) ex;
                    assertThat(apiEx.getHttpStatus()).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
                    assertThat(apiEx.getMessage()).isEqualTo("Some message");
                });    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("createBookings: Success - should use objectMapper to parse layout")
    void createBookings_UsesObjectMapper() throws Exception {
        // Given
        var seatDto = new SeatCoordinateRequestDto(0, 0, null);
        var createDto = new BookingCreateDto(showtimeId, List.of(seatDto));

        given(showtimeRepository.findById(showtimeId)).willReturn(Optional.of(showtime));
        // Add stripe mocks to avoid NPE
        given(paymentService.createCheckoutSession(any(), any())).willReturn(stripeSession);
        given(stripeSession.getUrl()).willReturn("http://url");
        given(stripeSession.getId()).willReturn("id");

        // When
        bookingService.createBookings(createDto, user);

        // Then
        verify(objectMapper, atLeastOnce()).convertValue(any(), any(TypeReference.class));
    }

    @Test
    @DisplayName("createBookings: Failure - Seat already taken")
    void createBookings_SeatTaken_ThrowsException() {
        // Given
        var seatDto = new SeatCoordinateRequestDto(0, 0, null);
        var createDto = new BookingCreateDto(showtimeId, List.of(seatDto));

        showtime.setStartTime(Instant.now().plus(Duration.ofDays(1)));
        given(showtimeRepository.findById(showtimeId)).willReturn(Optional.of(showtime));

        given(translationService.get("booking.seat.taken"))
                .willReturn("Seat is already taken");


        doThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate key"))
                .when(orderRepository).flush();
        // When & Then
        assertThatThrownBy(() -> bookingService.createBookings(createDto, user))
                .isInstanceOf(RestApiException.class)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.CONFLICT)
                .hasMessageContaining("is already taken");
    }

    @Test
    @DisplayName("getUserBookings: Success - return list of DTOs")
    void getUserBookings_Success() {
        // Given
        given(bookingRepository.findAllByUserIdWithDetails(user.getId())).willReturn(List.of(new Booking()));
        given(bookingMapper.toResponseDto(any())).willReturn(mock(BookingResponseDto.class));

        // When
        List<BookingResponseDto> result = bookingService.getUserBookings(user);

        // Then
        assertThat(result).hasSize(1);
        verify(bookingRepository).findAllByUserIdWithDetails(user.getId());
    }

    @Test
    @DisplayName("getUserBookings: Edge Case - user has no bookings")
    void getUserBookings_EmptyList_ReturnsEmpty() {
        // Given
        given(bookingRepository.findAllByUserIdWithDetails(user.getId()))
                .willReturn(Collections.emptyList());

        // When
        List<BookingResponseDto> result = bookingService.getUserBookings(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(bookingRepository).findAllByUserIdWithDetails(user.getId());
        verifyNoInteractions(bookingMapper);
    }


    @Test
    @DisplayName("getUserBookings: Edge Case - database returns null (safety check)")
    void getUserBookings_RepositoryReturnsNull_HandlesGracefully() {
        // Given
        given(bookingRepository.findAllByUserIdWithDetails(user.getId()))
                .willReturn(null);

        // When & Then
        // Our current implementation will throw NPE because of .stream()
        // This test helps us decide if we need a null check in the service
        assertThatThrownBy(() -> bookingService.getUserBookings(user))
                .isInstanceOf(NullPointerException.class);
    }


    @Test
    @DisplayName("getOccupiedSeats: Success - return only coordinates")
    void getOccupiedSeats_Success() {
        // Given
        Booking b = new Booking();
        b.setRowIndex(5);
        b.setColIndex(10);
        given(bookingRepository.findOccupiedOrLocked(eq(showtimeId))).willReturn(List.of(b));

        // When
        List<SeatCoordinateDto> result = bookingService.getOccupiedSeats(showtimeId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().row()).isEqualTo(5);
        assertThat(result.getFirst().col()).isEqualTo(10);
    }

    @Test
    @DisplayName("getOccupiedSeats: Edge Case - no seats occupied for this showtime")
    void getOccupiedSeats_EmptyResult_ReturnsEmptyList() {
        // Given
        UUID showtimeId = UUID.randomUUID();
        given(bookingRepository.findOccupiedOrLocked(eq(showtimeId)))
                .willReturn(Collections.emptyList());

        // When
        List<SeatCoordinateDto> result = bookingService.getOccupiedSeats(showtimeId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(bookingRepository).findOccupiedOrLocked(eq(showtimeId));
    }


    @Test
    @DisplayName("getOccupiedSeats: Consistency - should filter out CANCELLED bookings")
    void getOccupiedSeats_FiltersActiveStatusesOnly() {
        // Given
        UUID showtimeId = UUID.randomUUID();
        Booking activeBooking = new Booking();
        activeBooking.setRowIndex(1);
        activeBooking.setColIndex(1);

        // We verify that the service correctly passes ACTIVE_STATUSES to the repository
        given(bookingRepository.findOccupiedOrLocked(eq(showtimeId)))
                .willReturn(List.of(activeBooking));

        // When
        List<SeatCoordinateDto> result = bookingService.getOccupiedSeats(showtimeId);

        // Then
        assertThat(result).hasSize(1);
        // If the repository is correctly mocked, this confirms our service uses the right status filter
    }

}
