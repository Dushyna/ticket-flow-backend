package io.github.dushyna.ticketflow.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dushyna.ticketflow.booking.dto.request.BookingCreateDto;
import io.github.dushyna.ticketflow.booking.dto.request.SeatCoordinateRequestDto;
import io.github.dushyna.ticketflow.booking.dto.response.BookingResponseDto;
import io.github.dushyna.ticketflow.booking.dto.response.SeatCoordinateDto;
import io.github.dushyna.ticketflow.booking.service.interfaces.BookingService;
import io.github.dushyna.ticketflow.security.dto.AuthUserDetails;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;

    private AuthUserDetails userDetails;
    private final UUID SHOWTIME_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        AppUser user = new AppUser();
        user.setEmail("buyer@test.com");
        user.setRole(Role.ROLE_USER);
        userDetails = new AuthUserDetails(user);
    }

    @Test
    @DisplayName("POST /api/v1/bookings - Success: Create bookings for authenticated user")
    void createBooking_Success() throws Exception {
        // Given
        SeatCoordinateRequestDto seat = new SeatCoordinateRequestDto(5, 10, UUID.randomUUID());
        BookingCreateDto requestDto = new BookingCreateDto(SHOWTIME_ID, List.of(seat));

        // When & Then
        mockMvc.perform(post("/api/v1/bookings")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated());

        verify(bookingService, times(1)).createBookings(any(BookingCreateDto.class), any(AppUser.class));
    }

    @Test
    @DisplayName("POST /api/v1/bookings - Failure: Conflict if seats are taken")
    void createBooking_Conflict() throws Exception {
        // Given: Create a VALID DTO so it passes validation (needs at least one seat)
        SeatCoordinateRequestDto seat = new SeatCoordinateRequestDto(1, 1, UUID.randomUUID());
        BookingCreateDto requestDto = new BookingCreateDto(SHOWTIME_ID, List.of(seat));

        // Mock service to throw Conflict ONLY when validation is passed
        doThrow(new io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException(
                HttpStatus.CONFLICT, "Seats already taken"))
                .when(bookingService).createBookings(any(BookingCreateDto.class), any(AppUser.class));

        // When & Then
        mockMvc.perform(post("/api/v1/bookings")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isConflict()); // Now it will reach 409
    }

    @Test
    @DisplayName("POST /api/v1/bookings - Failure: Validation error (Empty seats list)")
    void createBooking_ValidationFailed() throws Exception {
        // Given: seats list is empty, violates @NotEmpty
        BookingCreateDto invalidDto = new BookingCreateDto(SHOWTIME_ID, List.of());

        mockMvc.perform(post("/api/v1/bookings")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/bookings - Failure: Validation error (Missing showtimeId)")
    void createBooking_MissingShowtimeId_ReturnsBadRequest() throws Exception {
        // Given: showtimeId is null
        SeatCoordinateRequestDto seat = new SeatCoordinateRequestDto(1, 1, UUID.randomUUID());
        BookingCreateDto invalidDto = new BookingCreateDto(null, List.of(seat));

        mockMvc.perform(post("/api/v1/bookings")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("POST /api/v1/bookings - Failure: Validation error (Invalid seat coordinates)")
    void createBooking_InvalidSeat_ReturnsBadRequest() throws Exception {
        // Given: Row or column is 0 (assuming @Min(1) in SeatCoordinateRequestDto)
        // noinspection ConstantConditions
        SeatCoordinateRequestDto invalidSeat = new SeatCoordinateRequestDto(-1, 0, UUID.randomUUID());
        BookingCreateDto invalidDto = new BookingCreateDto(SHOWTIME_ID, List.of(invalidSeat));

        mockMvc.perform(post("/api/v1/bookings")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/bookings - Failure: Unauthorized for anonymous user")
    void createBooking_Anonymous_ReturnsUnauthorized() throws Exception {
        // Given
        SeatCoordinateRequestDto seat = new SeatCoordinateRequestDto(1, 1, UUID.randomUUID());
        BookingCreateDto requestDto = new BookingCreateDto(SHOWTIME_ID, List.of(seat));

        // When & Then
        mockMvc.perform(post("/api/v1/bookings")
                        .with(csrf()) // Still need CSRF, but no user(...)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                // Change expected status from 403 to 401
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/bookings - Failure: Missing CSRF token")
    void createBooking_MissingCsrf_ReturnsForbidden() throws Exception {
        SeatCoordinateRequestDto seat = new SeatCoordinateRequestDto(1, 1, UUID.randomUUID());
        BookingCreateDto requestDto = new BookingCreateDto(SHOWTIME_ID, List.of(seat));

        mockMvc.perform(post("/api/v1/bookings")
                        .with(user(userDetails))
                        // Missing .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());
    }


    @Test
    @DisplayName("GET /api/v1/bookings/occupied/{showtimeId} - Success: Public access")
    void getOccupied_Public_Success() throws Exception {
        // Given
        SeatCoordinateDto occupiedSeat = new SeatCoordinateDto(1, 1);
        when(bookingService.getOccupiedSeats(eq(SHOWTIME_ID))).thenReturn(List.of(occupiedSeat));

        // When & Then
        mockMvc.perform(get("/api/v1/bookings/occupied/{showtimeId}", SHOWTIME_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].row").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/bookings/occupied/{showtimeId} - Success: Returns empty list if no seats are taken")
    void getOccupied_EmptyList_ReturnsOk() throws Exception {
        // Given: Showtime exists but no bookings yet
        when(bookingService.getOccupiedSeats(eq(SHOWTIME_ID))).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/bookings/occupied/{showtimeId}", SHOWTIME_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/bookings/occupied/{showtimeId} - Failure: Invalid showtime ID format")
    void getOccupied_InvalidUuid_ReturnsBadRequest() throws Exception {
        // When & Then: Passing string instead of UUID
        mockMvc.perform(get("/api/v1/bookings/occupied/{showtimeId}", "not-a-uuid-format"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/bookings/occupied/{showtimeId} - Failure: Showtime not found")
    void getOccupied_NotFound_Returns404() throws Exception {
        // Given: Service throws exception for missing showtime
        UUID missingId = UUID.randomUUID();
        when(bookingService.getOccupiedSeats(eq(missingId)))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("Showtime not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/bookings/occupied/{showtimeId}", missingId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/bookings/my - Success: Get personal bookings")
    void getMyBookings_Success() throws Exception {
        // Given
        BookingResponseDto booking = new BookingResponseDto(
                UUID.randomUUID(), "Inception", "Hall A", Instant.now(),
                new SeatCoordinateDto(5, 5), "Adult", new BigDecimal("15.00"), "CONFIRMED"
        );
        when(bookingService.getUserBookings(any(AppUser.class))).thenReturn(List.of(booking));

        // When & Then
        mockMvc.perform(get("/api/v1/bookings/my")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].movieTitle").value("Inception"));
    }

    @Test
    @DisplayName("GET /api/v1/bookings/my - Failure: Forbidden for anonymous")
    void getMyBookings_Anonymous_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/bookings/my"))
                .andDo(print())
                // Use isForbidden() because Spring Security returns 403 for this endpoint
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/bookings/my - Success: Returns empty list if user has no bookings")
    void getMyBookings_EmptyList_ReturnsOk() throws Exception {
        // Given: Service returns an empty list for this user
        when(bookingService.getUserBookings(any(AppUser.class))).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/bookings/my")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/bookings/my - Failure: Internal server error from service")
    void getMyBookings_ServiceError_ReturnsInternalServerError() throws Exception {
        // Given
        when(bookingService.getUserBookings(any(AppUser.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(get("/api/v1/bookings/my")
                        .with(user(userDetails)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /api/v1/bookings/my - Success: Verify nested seat coordinates")
    void getMyBookings_VerifyNestedData() throws Exception {
        // Given
        BookingResponseDto booking = new BookingResponseDto(
                UUID.randomUUID(), "Inception", "Hall A", Instant.now(),
                new SeatCoordinateDto(7, 8), "Adult", new BigDecimal("15.00"), "CONFIRMED"
        );
        when(bookingService.getUserBookings(any(AppUser.class))).thenReturn(List.of(booking));

        // When & Then
        mockMvc.perform(get("/api/v1/bookings/my")
                        .with(user(userDetails)))
                .andExpect(jsonPath("$[0].seat.row").value(7))
                .andExpect(jsonPath("$[0].seat.col").value(8));
    }


}
