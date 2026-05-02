package io.github.dushyna.ticketflow.cinema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dushyna.ticketflow.cinema.dto.request.MovieHallCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.MovieHallResponseDto;
import io.github.dushyna.ticketflow.cinema.service.interfaces.MovieHallService;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MovieHallControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MovieHallService hallService;

    private AuthUserDetails adminDetails;
    private AuthUserDetails regularDetails;
    private final UUID CINEMA_ID = UUID.randomUUID();
    private final UUID HALL_ID = UUID.randomUUID();

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
    @DisplayName("POST /api/v1/halls - Success: Create hall as TENANT_ADMIN")
    void createHall_Success() throws Exception {
        // Given
        MovieHallCreateDto requestDto = new MovieHallCreateDto("IMAX Grand", CINEMA_ID, 10, 12, Map.of("grid", "data"));
        MovieHallResponseDto responseDto = new MovieHallResponseDto(HALL_ID, "IMAX Grand", CINEMA_ID, UUID.randomUUID(), 10, 12, Map.of("grid", "data"));

        when(hallService.createHall(any(MovieHallCreateDto.class), any(AppUser.class))).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(post("/api/v1/halls")
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(HALL_ID.toString()))
                .andExpect(jsonPath("$.name").value("IMAX Grand"));
    }

    @Test
    @DisplayName("POST /api/v1/halls - Failure: Validation error (Blank Name)")
    void createHall_BlankName_ReturnsBadRequest() throws Exception {
        // Given: name is blank
        MovieHallCreateDto invalidRequest = new MovieHallCreateDto("", CINEMA_ID, 10, 10, Map.of());

        // When & Then
        mockMvc.perform(post("/api/v1/halls")
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/halls - Failure: Forbidden for regular USER")
    void createHall_RegularUser_ReturnsForbidden() throws Exception {
        // Given: Valid data, but user lacks TENANT_ADMIN role
        MovieHallCreateDto requestDto = new MovieHallCreateDto(
                "IMAX Grand", CINEMA_ID, 10, 12, Map.of("grid", "data")
        );

        // When & Then
        mockMvc.perform(post("/api/v1/halls")
                        .with(user(regularDetails)) // Authenticated as ROLE_USER
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isForbidden());

        // Verify that service method was never reached due to security interceptor
        verify(hallService, never()).createHall(any(), any());
    }


    @Test
    @DisplayName("GET /api/v1/halls/cinema/{cinemaId} - Success: Get all halls (Public)")
    void getAllByCinema_Success() throws Exception {
        // Given
        MovieHallResponseDto hall = new MovieHallResponseDto(HALL_ID, "Hall 1", CINEMA_ID, UUID.randomUUID(), 5, 5, Map.of());
        when(hallService.getAllByCinema(eq(CINEMA_ID), any())).thenReturn(List.of(hall));

        // When & Then
        mockMvc.perform(get("/api/v1/halls/cinema/{cinemaId}", CINEMA_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Hall 1"));
    }
    @Test
    @DisplayName("GET /api/v1/halls/cinema/{cinemaId} - Failure: Cinema not found")
    void getAllByCinema_NotFound_Returns404() throws Exception {
        // Given: Service throws EntityNotFoundException when cinema doesn't exist
        UUID nonExistentCinemaId = UUID.randomUUID();
        when(hallService.getAllByCinema(eq(nonExistentCinemaId), any()))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("Cinema not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/halls/cinema/{cinemaId}", nonExistentCinemaId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/halls/cinema/{cinemaId} - Success: Empty list if no halls exist")
    void getAllByCinema_EmptyList_Returns200() throws Exception {
        // Given: Cinema exists but has no halls
        when(hallService.getAllByCinema(eq(CINEMA_ID), any())).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/halls/cinema/{cinemaId}", CINEMA_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/halls/cinema/{cinemaId} - Failure: Invalid UUID format")
    void getAllByCinema_InvalidUuid_ReturnsBadRequest() throws Exception {
        // When & Then: Providing a string that is not a UUID
        mockMvc.perform(get("/api/v1/halls/cinema/{cinemaId}", "not-a-uuid-format"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/halls/cinema/{cinemaId} - Success: Authenticated user can also access")
    void getAllByCinema_AuthenticatedUser_ReturnsOk() throws Exception {
        // Given: Even though it's permitAll, we check if it works with security context
        MovieHallResponseDto hall = new MovieHallResponseDto(
                UUID.randomUUID(), "VIP Hall", CINEMA_ID, UUID.randomUUID(), 5, 5, Map.of()
        );
        when(hallService.getAllByCinema(eq(CINEMA_ID), any())).thenReturn(List.of(hall));

        // When & Then
        mockMvc.perform(get("/api/v1/halls/cinema/{cinemaId}", CINEMA_ID)
                        .with(user(regularDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("VIP Hall"));
    }

    @Test
    @DisplayName("GET /api/v1/halls/{id} - Success: Get hall by ID (Public)")
    void getById_Success() throws Exception {
        // Given
        MovieHallResponseDto responseDto = new MovieHallResponseDto(
                HALL_ID, "IMAX Grand", CINEMA_ID, UUID.randomUUID(), 10, 12, Map.of("grid", "data")
        );

        when(hallService.getByIdOrThrow(eq(HALL_ID), any())).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/v1/halls/{id}", HALL_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(HALL_ID.toString()))
                .andExpect(jsonPath("$.name").value("IMAX Grand"));
    }

    @Test
    @DisplayName("GET /api/v1/halls/{id} - Failure: Hall not found")
    void getById_NotFound_Returns404() throws Exception {
        // Given: Service throws exception if ID does not exist
        UUID randomId = UUID.randomUUID();
        when(hallService.getByIdOrThrow(eq(randomId), any()))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("Hall not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/halls/{id}", randomId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/halls/{id} - Failure: Invalid UUID format")
    void getById_InvalidUuid_ReturnsBadRequest() throws Exception {
        // When & Then: Passing an invalid string instead of a UUID
        mockMvc.perform(get("/api/v1/halls/{id}", "invalid-uuid-format"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/halls/{id} - Success: Accessible for authenticated admin")
    void getById_AsAdmin_ReturnsOk() throws Exception {
        // Given: Ensure that @PreAuthorize("permitAll()") works fine with authentication context
        MovieHallResponseDto responseDto = new MovieHallResponseDto(
                HALL_ID, "Admin View", CINEMA_ID, UUID.randomUUID(), 10, 12, Map.of()
        );
        when(hallService.getByIdOrThrow(eq(HALL_ID), any())).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/v1/halls/{id}", HALL_ID)
                        .with(user(adminDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Admin View"));
    }

    @Test
    @DisplayName("PATCH /api/v1/halls/{id} - Success: Update hall")
    void updateHall_Success() throws Exception {
        // Given
        MovieHallCreateDto updateDto = new MovieHallCreateDto("Updated Name", CINEMA_ID, 8, 8, Map.of());
        MovieHallResponseDto responseDto = new MovieHallResponseDto(HALL_ID, "Updated Name", CINEMA_ID, UUID.randomUUID(), 8, 8, Map.of());

        when(hallService.updateHall(eq(HALL_ID), any(MovieHallCreateDto.class), any(AppUser.class))).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(patch("/api/v1/halls/{id}", HALL_ID)
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    @DisplayName("PATCH /api/v1/halls/{id} - Failure: Forbidden for regular USER")
    void updateHall_RegularUser_ReturnsForbidden() throws Exception {
        // Given: Valid DTO but insufficient permissions
        MovieHallCreateDto updateDto = new MovieHallCreateDto("New Name", CINEMA_ID, 10, 10, Map.of());

        // When & Then
        mockMvc.perform(patch("/api/v1/halls/{id}", HALL_ID)
                        .with(user(regularDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(hallService, never()).updateHall(any(), any(), any());
    }

    @Test
    @DisplayName("PATCH /api/v1/halls/{id} - Failure: Hall not found")
    void updateHall_NotFound_Returns404() throws Exception {
        // Given: Service throws EntityNotFoundException
        MovieHallCreateDto updateDto = new MovieHallCreateDto("New Name", CINEMA_ID, 10, 10, Map.of());
        when(hallService.updateHall(eq(HALL_ID), any(MovieHallCreateDto.class), any(AppUser.class)))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("Hall not found"));

        // When & Then
        mockMvc.perform(patch("/api/v1/halls/{id}", HALL_ID)
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/v1/halls/{id} - Failure: Validation error (Columns count < 1)")
    void updateHall_InvalidCols_ReturnsBadRequest() throws Exception {
        // Given: cols is 0, which violates @Min(1)
        // noinspection ConstantConditions
        MovieHallCreateDto invalidDto = new MovieHallCreateDto("Update", CINEMA_ID, 10, 0, Map.of());

        // When & Then
        mockMvc.perform(patch("/api/v1/halls/{id}", HALL_ID)
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/halls/{id} - Failure: Malformed JSON body")
    void updateHall_MalformedJson_ReturnsBadRequest() throws Exception {
        // Missing closing quote for the value AND missing closing brace for the object
        String malformedJson = "{ \"name\": \"Unclosed JSON ";

        // When & Then
        mockMvc.perform(patch("/api/v1/halls/{id}", HALL_ID)
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andDo(print())
                // Jackson throws HttpMessageNotReadableException,
                // which should be mapped to 400 Bad Request
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/halls/{id} - Failure: Invalid Hall ID format")
    void updateHall_InvalidIdFormat_ReturnsBadRequest() throws Exception {
        MovieHallCreateDto updateDto = new MovieHallCreateDto("Name", CINEMA_ID, 5, 5, Map.of());

        // When & Then: Passing 'abc' as UUID
        mockMvc.perform(patch("/api/v1/halls/{id}", "abc")
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/v1/halls/{id} - Success: Delete hall")
    void deleteHall_Success() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/halls/{id}", HALL_ID)
                        .with(user(adminDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(hallService, times(1)).deleteHall(eq(HALL_ID), any(AppUser.class));
    }


    @Test
    @DisplayName("DELETE /api/v1/halls/{id} - Failure: Forbidden for regular USER")
    void deleteHall_RegularUser_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/halls/{id}", HALL_ID)
                        .with(user(regularDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/halls/{id} - Failure: Hall not found")
    void deleteHall_NotFound_Returns404() throws Exception {
        // Given: Service throws EntityNotFoundException if the hall doesn't exist
        UUID missingId = UUID.randomUUID();
        doThrow(new jakarta.persistence.EntityNotFoundException("Hall not found"))
                .when(hallService).deleteHall(eq(missingId), any(AppUser.class));

        // When & Then
        mockMvc.perform(delete("/api/v1/halls/{id}", missingId)
                        .with(user(adminDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/halls/{id} - Failure: Unauthorized for Anonymous")
    void deleteHall_Anonymous_ReturnsUnauthorized() throws Exception {
        // When & Then: Request without .with(user(...))
        mockMvc.perform(delete("/api/v1/halls/{id}", HALL_ID)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        // Verify service was never called
        verify(hallService, never()).deleteHall(any(), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/halls/{id} - Failure: Invalid UUID format")
    void deleteHall_InvalidUuid_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/halls/{id}", "not-a-uuid")
                        .with(user(adminDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

}
