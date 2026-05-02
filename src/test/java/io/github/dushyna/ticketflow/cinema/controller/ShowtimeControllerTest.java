package io.github.dushyna.ticketflow.cinema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dushyna.ticketflow.cinema.dto.request.ShowtimeCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.ShowtimeResponseDto;
import io.github.dushyna.ticketflow.cinema.service.interfaces.ShowtimeService;
import io.github.dushyna.ticketflow.security.dto.AuthUserDetails;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.eq;


@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShowtimeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShowtimeService showtimeService;

    private AuthUserDetails adminDetails;
    private AuthUserDetails regularDetails;
    private final UUID MOVIE_ID = UUID.randomUUID();
    private final UUID HALL_ID = UUID.randomUUID();
    private final UUID SHOWTIME_ID = UUID.randomUUID();
    private final Instant FUTURE_START = Instant.now().plus(1, ChronoUnit.DAYS);

    @BeforeEach
    void setUp() {
        AppUser admin = new AppUser();
        admin.setEmail("admin@cinema.com");
        admin.setRole(Role.ROLE_TENANT_ADMIN);
        adminDetails = new AuthUserDetails(admin);

        AppUser regularUser = new AppUser();
        regularUser.setEmail("user@test.com");
        regularUser.setRole(Role.ROLE_USER);
        regularDetails = new AuthUserDetails(regularUser);
    }

    @Test
    @DisplayName("POST /api/v1/showtimes - Success: Create showtime with valid price and Instant")
    void createShowtime_Success() throws Exception {
        // Given
        ShowtimeCreateDto requestDto = new ShowtimeCreateDto(
                MOVIE_ID, HALL_ID, FUTURE_START, new BigDecimal("150.00")
        );

        ShowtimeResponseDto responseDto = new ShowtimeResponseDto(
                UUID.randomUUID(), MOVIE_ID, "Interstellar", HALL_ID, "IMAX Hall 1",
                FUTURE_START, FUTURE_START.plus(3, ChronoUnit.HOURS), new BigDecimal("150.00")
        );

        when(showtimeService.createShowtime(any(ShowtimeCreateDto.class), any(AppUser.class)))
                .thenReturn(responseDto);

        // When & Then
        mockMvc.perform(post("/api/v1/showtimes")
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movieTitle").value("Interstellar"))
                .andExpect(jsonPath("$.basePrice").value(150.00));
    }

    @Test
    @DisplayName("POST /api/v1/showtimes - Failure: Validation error (Price is 0.0)")
    void createShowtime_InvalidPrice_ReturnsBadRequest() throws Exception {
        // Given: basePrice is 0.0, violates @DecimalMin(value = "0.0", inclusive = false)
        ShowtimeCreateDto invalidRequest = new ShowtimeCreateDto(
                MOVIE_ID, HALL_ID, FUTURE_START, new BigDecimal("0.0")
        );

        // When & Then
        mockMvc.perform(post("/api/v1/showtimes")
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/showtimes - Failure: Validation error (Missing fields)")
    void createShowtime_MissingFields_ReturnsBadRequest() throws Exception {
        // Given: Missing movieId and startTime
        ShowtimeCreateDto invalidRequest = new ShowtimeCreateDto(
                null, HALL_ID, null, new BigDecimal("100.00")
        );

        // When & Then
        mockMvc.perform(post("/api/v1/showtimes")
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/showtimes - Failure: Validation error (Negative price)")
    void createShowtime_NegativePrice_ReturnsBadRequest() throws Exception {
        // Given: negative price
        ShowtimeCreateDto invalidRequest = new ShowtimeCreateDto(
                MOVIE_ID, HALL_ID, FUTURE_START, new BigDecimal("-5.00")
        );

        // When & Then
        mockMvc.perform(post("/api/v1/showtimes")
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/showtimes/{id} - Success: Update showtime details")
    void updateShowtime_Success() throws Exception {
        // Given
        ShowtimeCreateDto updateDto = new ShowtimeCreateDto(
                MOVIE_ID, HALL_ID, FUTURE_START, new BigDecimal("200.00")
        );
        ShowtimeResponseDto responseDto = new ShowtimeResponseDto(
                SHOWTIME_ID, MOVIE_ID, "Interstellar", HALL_ID, "IMAX Hall 1",
                FUTURE_START, FUTURE_START.plus(3, ChronoUnit.HOURS), new BigDecimal("200.00")
        );

        when(showtimeService.updateShowtime(eq(SHOWTIME_ID), any(ShowtimeCreateDto.class), any(AppUser.class)))
                .thenReturn(responseDto);

        // When & Then
        mockMvc.perform(patch("/api/v1/showtimes/{id}", SHOWTIME_ID)
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basePrice").value(200.00))
                .andExpect(jsonPath("$.startTime").exists());
    }

    @Test
    @DisplayName("PATCH /api/v1/showtimes/{id} - Failure: Conflict (Overlapping session)")
    void updateShowtime_OverlapConflict_Returns409() throws Exception {
        // Given
        ShowtimeCreateDto updateDto = new ShowtimeCreateDto(MOVIE_ID, HALL_ID, FUTURE_START, new BigDecimal("150.00"));

        when(showtimeService.updateShowtime(any(), any(), any()))
                .thenThrow(new io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException(
                        org.springframework.http.HttpStatus.CONFLICT, "New timing overlaps with another session"));

        // When & Then
        mockMvc.perform(patch("/api/v1/showtimes/{id}", SHOWTIME_ID)
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /api/v1/showtimes/{id} - Failure: Validation error (Price too low)")
    void updateShowtime_InvalidPrice_ReturnsBadRequest() throws Exception {
        // Given: Price is 0.0, violates @DecimalMin(value = "0.0", inclusive = false)
        ShowtimeCreateDto invalidDto = new ShowtimeCreateDto(MOVIE_ID, HALL_ID, FUTURE_START, BigDecimal.ZERO);

        // When & Then
        mockMvc.perform(patch("/api/v1/showtimes/{id}", SHOWTIME_ID)
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/showtimes/{id} - Failure: Showtime not found")
    void updateShowtime_NotFound_Returns404() throws Exception {
        // Given
        ShowtimeCreateDto updateDto = new ShowtimeCreateDto(MOVIE_ID, HALL_ID, FUTURE_START, new BigDecimal("100.00"));

        when(showtimeService.updateShowtime(any(), any(), any()))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("Showtime not found"));

        // When & Then
        mockMvc.perform(patch("/api/v1/showtimes/{id}", UUID.randomUUID())
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/v1/showtimes/{id} - Failure: Forbidden for regular USER")
    void updateShowtime_RegularUser_ReturnsForbidden() throws Exception {
        ShowtimeCreateDto updateDto = new ShowtimeCreateDto(MOVIE_ID, HALL_ID, FUTURE_START, new BigDecimal("100.00"));

        mockMvc.perform(patch("/api/v1/showtimes/{id}", SHOWTIME_ID)
                        .with(user(regularDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(showtimeService, never()).updateShowtime(any(), any(), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/showtimes/{id} - Success: Delete showtime as TENANT_ADMIN")
    void deleteShowtime_Success() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/showtimes/{id}", SHOWTIME_ID)
                        .with(user(adminDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNoContent());

        // Verify that the service method was called with correct parameters
        verify(showtimeService, times(1)).deleteShowtime(eq(SHOWTIME_ID), any(AppUser.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/showtimes/{id} - Failure: Forbidden for regular USER")
    void deleteShowtime_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/showtimes/{id}", SHOWTIME_ID)
                        .with(user(regularDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());

        // Verify service was never called
        verify(showtimeService, never()).deleteShowtime(any(), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/showtimes/{id} - Failure: Not Found")
    void deleteShowtime_NotFound_Returns404() throws Exception {
        // Given: Service throws EntityNotFoundException
        doThrow(new jakarta.persistence.EntityNotFoundException("Showtime not found"))
                .when(showtimeService).deleteShowtime(eq(SHOWTIME_ID), any(AppUser.class));

        // When & Then
        mockMvc.perform(delete("/api/v1/showtimes/{id}", SHOWTIME_ID)
                        .with(user(adminDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/showtimes/{id} - Failure: Unauthorized for anonymous")
    void deleteShowtime_Unauthorized() throws Exception {
        // When & Then: No user() provided
        mockMvc.perform(delete("/api/v1/showtimes/{id}", SHOWTIME_ID)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/showtimes/{id} - Success: Get showtime by ID (Public Access)")
    void getById_Public_Success() throws Exception {
        // Given
        ShowtimeResponseDto responseDto = new ShowtimeResponseDto(
                SHOWTIME_ID, MOVIE_ID, "Interstellar", HALL_ID, "IMAX Hall 1",
                Instant.now(), Instant.now().plus(3, ChronoUnit.HOURS), new BigDecimal("150.00")
        );

        when(showtimeService.getByIdOrThrow(eq(SHOWTIME_ID))).thenReturn(responseDto);

        // When & Then: No .with(user(...)) needed as it's public
        mockMvc.perform(get("/api/v1/showtimes/{id}", SHOWTIME_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SHOWTIME_ID.toString()))
                .andExpect(jsonPath("$.movieTitle").value("Interstellar"));
    }

    @Test
    @DisplayName("GET /api/v1/showtimes/{id} - Success: Get showtime by ID (Authenticated User)")
    void getById_Authenticated_Success() throws Exception {
        // Given
        ShowtimeResponseDto responseDto = new ShowtimeResponseDto(
                SHOWTIME_ID, MOVIE_ID, "Interstellar", HALL_ID, "IMAX Hall 1",
                Instant.now(), Instant.now().plus(3, ChronoUnit.HOURS), new BigDecimal("150.00")
        );

        when(showtimeService.getByIdOrThrow(eq(SHOWTIME_ID))).thenReturn(responseDto);

        // When & Then: Even with a user, it should work fine
        mockMvc.perform(get("/api/v1/showtimes/{id}", SHOWTIME_ID)
                        .with(user(regularDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SHOWTIME_ID.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/showtimes/{id} - Failure: Not Found")
    void getById_NotFound_Returns404() throws Exception {
        // Given
        UUID missingId = UUID.randomUUID();
        when(showtimeService.getByIdOrThrow(eq(missingId)))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("Showtime not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/showtimes/{id}", missingId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/showtimes/{id} - Failure: Invalid UUID format")
    void getById_InvalidUuid_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/showtimes/{id}", "invalid-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/showtimes/movie/{movieId} - Success: Returns list of showtimes")
    void getByMovie_Success() throws Exception {
        // Given
        ShowtimeResponseDto showtime = new ShowtimeResponseDto(
                SHOWTIME_ID, MOVIE_ID, "Interstellar", HALL_ID, "IMAX Hall 1",
                Instant.now(), Instant.now().plus(3, ChronoUnit.HOURS), new BigDecimal("150.00")
        );

        when(showtimeService.getShowtimesByMovie(eq(MOVIE_ID))).thenReturn(List.of(showtime));

        // When & Then: Public access check
        mockMvc.perform(get("/api/v1/showtimes/movie/{movieId}", MOVIE_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].movieTitle").value("Interstellar"))
                .andExpect(jsonPath("$[0].id").value(SHOWTIME_ID.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/showtimes/movie/{movieId} - Success: Returns empty list if no showtimes found")
    void getByMovie_EmptyList_Returns200() throws Exception {
        // Given: Movie exists or not, but has no scheduled sessions
        when(showtimeService.getShowtimesByMovie(eq(MOVIE_ID))).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/showtimes/movie/{movieId}", MOVIE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/showtimes/movie/{movieId} - Failure: Invalid Movie UUID format")
    void getByMovie_InvalidUuid_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/showtimes/movie/{movieId}", "not-a-uuid"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/showtimes/hall/{hallId} - Success: Returns list of showtimes for a specific hall")
    void getByHall_Success() throws Exception {
        // Given
        ShowtimeResponseDto showtime = new ShowtimeResponseDto(
                SHOWTIME_ID, MOVIE_ID, "Interstellar", HALL_ID, "IMAX Hall 1",
                Instant.now(), Instant.now().plus(3, ChronoUnit.HOURS), new BigDecimal("150.00")
        );

        when(showtimeService.getShowtimesByHall(eq(HALL_ID))).thenReturn(List.of(showtime));

        // When & Then
        mockMvc.perform(get("/api/v1/showtimes/hall/{hallId}", HALL_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                // Explicitly use .value() with the string representation
                .andExpect(jsonPath("$[0].hallId").value(HALL_ID.toString()))
                .andExpect(jsonPath("$[0].hallName").value("IMAX Hall 1"));
    }

    @Test
    @DisplayName("GET /api/v1/showtimes/hall/{hallId} - Success: Returns empty list if hall has no showtimes")
    void getByHall_EmptyList_Returns200() throws Exception {
        // Given: Hall exists but no sessions are scheduled there
        when(showtimeService.getShowtimesByHall(eq(HALL_ID))).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/showtimes/hall/{hallId}", HALL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/showtimes/hall/{hallId} - Failure: Invalid Hall UUID format")
    void getByHall_InvalidUuid_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/showtimes/hall/{hallId}", "wrong-uuid-format"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/showtimes/cinema/{cinemaId} - Success: Returns all showtimes for the cinema")
    void getByCinema_Success() throws Exception {
        // Given
        UUID cinemaId = UUID.randomUUID();
        ShowtimeResponseDto showtime = new ShowtimeResponseDto(
                SHOWTIME_ID, MOVIE_ID, "Interstellar", HALL_ID, "IMAX Hall 1",
                Instant.now(), Instant.now().plus(3, ChronoUnit.HOURS), new BigDecimal("150.00")
        );

        when(showtimeService.getShowtimesByCinema(eq(cinemaId))).thenReturn(List.of(showtime));

        // When & Then: Public access
        mockMvc.perform(get("/api/v1/showtimes/cinema/{cinemaId}", cinemaId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(SHOWTIME_ID.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/showtimes/cinema/{cinemaId} - Success: Returns empty list if cinema has no sessions")
    void getByCinema_EmptyList_Returns200() throws Exception {
        // Given
        UUID cinemaId = UUID.randomUUID();
        when(showtimeService.getShowtimesByCinema(eq(cinemaId))).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/showtimes/cinema/{cinemaId}", cinemaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/showtimes/cinema/{cinemaId} - Failure: Invalid Cinema UUID format")
    void getByCinema_InvalidUuid_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/showtimes/cinema/{cinemaId}", "not-a-valid-uuid"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

}
