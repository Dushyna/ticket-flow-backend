package io.github.dushyna.ticketflow.cinema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dushyna.ticketflow.cinema.dto.request.CinemaCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.CinemaResponseDto;
import io.github.dushyna.ticketflow.cinema.service.interfaces.CinemaService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CinemaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CinemaService cinemaService;

    private AuthUserDetails tenantAdminDetails;
    private AuthUserDetails regularUserDetails;

    @BeforeEach
    void setUp() {
        // Prepare Tenant Admin
        AppUser adminUser = new AppUser();
        adminUser.setEmail("admin@cinema.com");
        adminUser.setRole(Role.ROLE_TENANT_ADMIN);
        tenantAdminDetails = new AuthUserDetails(adminUser);

        // Prepare Regular User
        AppUser regularUser = new AppUser();
        regularUser.setEmail("user@test.com");
        regularUser.setRole(Role.ROLE_USER);
        regularUserDetails = new AuthUserDetails(regularUser);
    }

    @Test
    @DisplayName("POST /api/v1/cinemas - Success: Allowed for TENANT_ADMIN")
    void createCinema_Success() throws Exception {
        // Given
        CinemaCreateDto requestDto = new CinemaCreateDto("Grand Cinema", "123 Main St");
        CinemaResponseDto responseDto = new CinemaResponseDto(UUID.randomUUID(), "Grand Cinema", "123 Main St", UUID.randomUUID(), List.of());

        when(cinemaService.createCinema(any(CinemaCreateDto.class), any(AppUser.class))).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(post("/api/v1/cinemas")
                        .with(user(tenantAdminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Grand Cinema"));
    }

    @Test
    @DisplayName("POST /api/v1/cinemas - Failure: Forbidden for regular USER")
    void createCinema_RegularUser_ReturnsForbidden() throws Exception {
        CinemaCreateDto requestDto = new CinemaCreateDto("Grand Cinema", "123 Main St");

        mockMvc.perform(post("/api/v1/cinemas")
                        .with(user(regularUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/v1/cinemas/{id} - Success: Update by TENANT_ADMIN")
    void updateCinema_Success() throws Exception {
        // Given
        UUID cinemaId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        CinemaCreateDto updateDto = new CinemaCreateDto("Updated Cinema Name", "Updated Address 123");

        CinemaResponseDto responseDto = new CinemaResponseDto(
                cinemaId,
                "Updated Cinema Name",
                "Updated Address 123",
                organizationId,
                List.of()
        );

        // Mocking the service to return the updated cinema DTO
        when(cinemaService.updateCinema(eq(cinemaId), any(CinemaCreateDto.class), any(AppUser.class)))
                .thenReturn(responseDto);

        // When & Then
        mockMvc.perform(patch("/api/v1/cinemas/{id}", cinemaId)
                        .with(user(tenantAdminDetails)) // Authenticated as TENANT_ADMIN
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cinemaId.toString()))
                .andExpect(jsonPath("$.name").value("Updated Cinema Name"))
                .andExpect(jsonPath("$.address").value("Updated Address 123"));
    }

    @Test
    @DisplayName("PATCH /api/v1/cinemas/{id} - Failure: Forbidden for regular USER")
    void updateCinema_RegularUser_ReturnsForbidden() throws Exception {
        // Given
        UUID cinemaId = UUID.randomUUID();
        CinemaCreateDto updateDto = new CinemaCreateDto("New Name", "New Address");

        // When & Then
        mockMvc.perform(patch("/api/v1/cinemas/{id}", cinemaId)
                        .with(user(regularUserDetails)) // Role: ROLE_USER
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/v1/cinemas/{id} - Failure: Not Found")
    void updateCinema_NotFound_ReturnsNotFound() throws Exception {
        // Given
        UUID cinemaId = UUID.randomUUID();
        CinemaCreateDto updateDto = new CinemaCreateDto("New Name", "New Address");

        // Mocking service to throw exception when cinema doesn't exist
        when(cinemaService.updateCinema(eq(cinemaId), any(CinemaCreateDto.class), any(AppUser.class)))
                .thenThrow(new io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Cinema not found"));

        // When & Then
        mockMvc.perform(patch("/api/v1/cinemas/{id}", cinemaId)
                        .with(user(tenantAdminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/v1/cinemas/{id} - Failure: Invalid Data (Validation)")
    void updateCinema_BlankName_ReturnsBadRequest() throws Exception {
        // Given: Blank name violates @NotBlank in CinemaCreateDto
        UUID cinemaId = UUID.randomUUID();
        CinemaCreateDto invalidDto = new CinemaCreateDto("", "Some Address");

        // When & Then
        mockMvc.perform(patch("/api/v1/cinemas/{id}", cinemaId)
                        .with(user(tenantAdminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/cinemas/{id} - Edge Case: Invalid UUID format")
    void updateCinema_InvalidUuid_ReturnsBadRequest() throws Exception {
        // When & Then: Providing a string that is not a UUID
        mockMvc.perform(patch("/api/v1/cinemas/{id}", "not-a-uuid")
                        .with(user(tenantAdminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CinemaCreateDto("Name", "Addr"))))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/cinemas/{id} - Failure: Validation error (empty name)")
    void updateCinema_InvalidDto_ReturnsBadRequest() throws Exception {
        // Given: Empty name violates @NotBlank
        CinemaCreateDto invalidDto = new CinemaCreateDto("", "Valid Address");
        UUID cinemaId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(patch("/api/v1/cinemas/{id}", cinemaId)
                        .with(user(tenantAdminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/cinemas - Success: Public access allowed")
    void getAllCinemas_Success() throws Exception {
        // Given
        CinemaResponseDto cinema = new CinemaResponseDto(UUID.randomUUID(), "Cinema 1", "Addr 1", UUID.randomUUID(), List.of());
        when(cinemaService.getAllForUser(any())).thenReturn(List.of(cinema));

        // When & Then
        mockMvc.perform(get("/api/v1/cinemas")) // No user needed (permitAll)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }


    @Test
    @DisplayName("GET /api/v1/cinemas - Failure: Service throws unexpected error")
    void getAllCinemas_InternalError_Returns500() throws Exception {
        // Given: Service encounters an unexpected database or logic error
        when(cinemaService.getAllForUser(any()))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(get("/api/v1/cinemas")
                        .with(user(regularUserDetails)))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    @Test
    @DisplayName("GET /api/v1/cinemas - Edge Case: Returns empty array instead of null")
    void getAllCinemas_EmptyList_ReturnsEmptyArray() throws Exception {
        // Given: No cinemas registered in the system
        when(cinemaService.getAllForUser(any())).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/cinemas")
                        .with(user(regularUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0))
                .andExpect(content().string("[]"));
    }

    @Test
    @DisplayName("GET /api/v1/cinemas/{id} - Success: Returns cinema details")
    void getCinemaById_Success() throws Exception {
        // Given
        UUID cinemaId = UUID.randomUUID();
        CinemaResponseDto responseDto = new CinemaResponseDto(
                cinemaId, "Cinema Star", "Main St 1", UUID.randomUUID(), List.of()
        );

        when(cinemaService.getByIdOrThrow(eq(cinemaId), any())).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/v1/cinemas/{id}", cinemaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cinemaId.toString()))
                .andExpect(jsonPath("$.name").value("Cinema Star"));
    }

    @Test
    @DisplayName("GET /api/v1/cinemas/{id} - Failure: Cinema Not Found")
    void getCinemaById_NotFound_Returns404() throws Exception {
        // Given
        UUID missingId = UUID.randomUUID();

        when(cinemaService.getByIdOrThrow(eq(missingId), any()))
                .thenThrow(new io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Cinema not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/cinemas/{id}", missingId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/v1/cinemas/{id} - Failure: Invalid UUID format")
    void getCinemaById_InvalidUuid_Returns400() throws Exception {
        // When & Then: Passing a string that is not a valid UUID
        mockMvc.perform(get("/api/v1/cinemas/{id}", "not-a-valid-uuid"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/cinemas/{id} - Success: Authenticated user can also access")
    void getCinemaById_Authenticated_ReturnsOk() throws Exception {
        // Given
        UUID cinemaId = UUID.randomUUID();
        CinemaResponseDto responseDto = new CinemaResponseDto(cinemaId, "Name", "Addr", UUID.randomUUID(), List.of());

        when(cinemaService.getByIdOrThrow(eq(cinemaId), any())).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/v1/cinemas/{id}", cinemaId)
                        .with(user(regularUserDetails))) // Accessing while logged in
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/cinemas/{id} - Success: Status 204 No Content")
    void deleteCinema_Success() throws Exception {
        // Given
        UUID cinemaId = UUID.randomUUID();
        doNothing().when(cinemaService).deleteCinema(eq(cinemaId), any(AppUser.class));

        // When & Then
        mockMvc.perform(delete("/api/v1/cinemas/{id}", cinemaId)
                        .with(user(tenantAdminDetails))
                .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/cinemas/{id} - Failure: Forbidden for regular USER")
    void deleteCinema_RegularUser_ReturnsForbidden() throws Exception {
        // Given
        UUID cinemaId = UUID.randomUUID();

        // When & Then: A standard user tries to delete a cinema
        mockMvc.perform(delete("/api/v1/cinemas/{id}", cinemaId)
                        .with(user(regularUserDetails)) // ROLE_USER
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/cinemas/{id} - Failure: Unauthorized for anonymous")
    void deleteCinema_Anonymous_ReturnsUnauthorized() throws Exception {
        // Given
        UUID cinemaId = UUID.randomUUID();

        // When & Then: Request without any auth context
        mockMvc.perform(delete("/api/v1/cinemas/{id}", cinemaId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/v1/cinemas/{id} - Failure: Cinema Not Found")
    void deleteCinema_NotFound_ReturnsNotFound() throws Exception {
        // Given
        UUID missingId = UUID.randomUUID();

        // Mocking service to throw exception if cinema doesn't exist or doesn't belong to admin
        doThrow(new io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Cinema not found"))
                .when(cinemaService).deleteCinema(eq(missingId), any(AppUser.class));

        // When & Then
        mockMvc.perform(delete("/api/v1/cinemas/{id}", missingId)
                        .with(user(tenantAdminDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/cinemas/{id} - Failure: Conflict (Has active halls)")
    void deleteCinema_HasHalls_ReturnsConflict() throws Exception {
        // Given
        UUID cinemaId = UUID.randomUUID();

        // Mocking service to throw 409 Conflict (e.g., integrity constraint)
        doThrow(new io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException(
                org.springframework.http.HttpStatus.CONFLICT, "Cannot delete cinema with existing movie halls"))
                .when(cinemaService).deleteCinema(eq(cinemaId), any(AppUser.class));

        // When & Then
        mockMvc.perform(delete("/api/v1/cinemas/{id}", cinemaId)
                        .with(user(tenantAdminDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

}
