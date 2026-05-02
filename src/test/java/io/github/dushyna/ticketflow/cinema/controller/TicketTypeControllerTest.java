package io.github.dushyna.ticketflow.cinema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dushyna.ticketflow.cinema.dto.request.TicketTypeRequestDto;
import io.github.dushyna.ticketflow.cinema.dto.response.TicketTypeResponseDto;
import io.github.dushyna.ticketflow.cinema.service.interfaces.TicketTypeService;
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
class TicketTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TicketTypeService ticketTypeService;

    private AuthUserDetails adminDetails;
    private AuthUserDetails regularDetails;

    private final UUID TICKET_TYPE_ID = UUID.randomUUID();
    private final UUID ORG_ID = UUID.randomUUID();

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
    @DisplayName("POST /api/v1/ticket-types - Success: Create ticket type")
    void createTicketType_Success() throws Exception {
        // Given
        TicketTypeRequestDto requestDto = new TicketTypeRequestDto("Child", new BigDecimal("0.50"), false);
        TicketTypeResponseDto responseDto = new TicketTypeResponseDto(TICKET_TYPE_ID, "Child", new BigDecimal("0.50"), false);

        when(ticketTypeService.createTicketType(any(TicketTypeRequestDto.class), any(AppUser.class))).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(post("/api/v1/ticket-types")
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("Child"))
                .andExpect(jsonPath("$.discount").value(0.50))
                .andExpect(jsonPath("$.isDefault").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/ticket-types - Failure: Validation error (Blank label)")
    void create_BlankLabel_ReturnsBadRequest() throws Exception {
        // Given: label is blank
        TicketTypeRequestDto invalidRequest = new TicketTypeRequestDto("", new BigDecimal("1.0"), false);

        mockMvc.perform(post("/api/v1/ticket-types")
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("POST /api/v1/ticket-types - Failure: Discount below minimum (0.0)")
    void create_DiscountTooLow_ReturnsBadRequest() throws Exception {
        // Given: discount is -0.1, violates @DecimalMin(value = "0.0")
        TicketTypeRequestDto invalidRequest = new TicketTypeRequestDto("Promo", new BigDecimal("-0.1"), false);

        mockMvc.perform(post("/api/v1/ticket-types")
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/ticket-types - Failure: Discount above maximum (10.0)")
    void create_DiscountTooHigh_ReturnsBadRequest() throws Exception {
        // Given: discount is 10.1, violates @DecimalMax(value = "10.0")
        TicketTypeRequestDto invalidRequest = new TicketTypeRequestDto("Extreme", new BigDecimal("10.1"), false);

        mockMvc.perform(post("/api/v1/ticket-types")
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/ticket-types - Failure: Discount is null")
    void create_NullDiscount_ReturnsBadRequest() throws Exception {
        // Given: discount is null, violates @NotNull
        TicketTypeRequestDto invalidRequest = new TicketTypeRequestDto("Student", null, false);

        mockMvc.perform(post("/api/v1/ticket-types")
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/ticket-types - Failure: Conflict (Label already exists)")
    void create_DuplicateLabel_ReturnsConflict() throws Exception {
        // Given
        TicketTypeRequestDto requestDto = new TicketTypeRequestDto("Adult", new BigDecimal("1.0"), false);

        when(ticketTypeService.createTicketType(any(), any()))
                .thenThrow(new io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException(
                        org.springframework.http.HttpStatus.CONFLICT, "Ticket type already exists"));

        // When & Then
        mockMvc.perform(post("/api/v1/ticket-types")
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/ticket-types - Failure: Unauthorized for anonymous")
    void create_Anonymous_ReturnsUnauthorized() throws Exception {
        TicketTypeRequestDto requestDto = new TicketTypeRequestDto("Adult", new BigDecimal("1.0"), false);

        mockMvc.perform(post("/api/v1/ticket-types")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /api/v1/ticket-types/{id} - Failure: Validation error (Discount too high)")
    void update_DiscountTooHigh_ReturnsBadRequest() throws Exception {
        // Given: Discount is 11.0, violates @DecimalMax("10.0")
        TicketTypeRequestDto invalidRequest = new TicketTypeRequestDto("VIP", new BigDecimal("11.0"), false);

        mockMvc.perform(patch("/api/v1/ticket-types/{id}", TICKET_TYPE_ID)
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/ticket-types/{id} - Success: Update ticket type")
    void updateTicketType_Success() throws Exception {
        // Given
        TicketTypeRequestDto updateDto = new TicketTypeRequestDto("Premium", new BigDecimal("1.50"), true);
        TicketTypeResponseDto responseDto = new TicketTypeResponseDto(TICKET_TYPE_ID, "Premium", new BigDecimal("1.50"), true);

        when(ticketTypeService.updateTicketType(eq(TICKET_TYPE_ID), any(TicketTypeRequestDto.class), any(AppUser.class)))
                .thenReturn(responseDto);

        // When & Then
        mockMvc.perform(patch("/api/v1/ticket-types/{id}", TICKET_TYPE_ID)
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Premium"))
                .andExpect(jsonPath("$.discount").value(1.50))
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    @DisplayName("PATCH /api/v1/ticket-types/{id} - Failure: Ticket type not found")
    void updateTicketType_NotFound_Returns404() throws Exception {
        // Given
        TicketTypeRequestDto updateDto = new TicketTypeRequestDto("Valid Label", new BigDecimal("1.0"), false);

        when(ticketTypeService.updateTicketType(any(), any(), any()))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("Ticket type not found"));

        // When & Then
        mockMvc.perform(patch("/api/v1/ticket-types/{id}", UUID.randomUUID())
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/v1/ticket-types/{id} - Failure: Forbidden for regular USER")
    void updateTicketType_RegularUser_ReturnsForbidden() throws Exception {
        TicketTypeRequestDto updateDto = new TicketTypeRequestDto("Standard", new BigDecimal("1.0"), false);

        mockMvc.perform(patch("/api/v1/ticket-types/{id}", TICKET_TYPE_ID)
                        .with(user(regularDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());

        verify(ticketTypeService, never()).updateTicketType(any(), any(), any());
    }

    @Test
    @DisplayName("PATCH /api/v1/ticket-types/{id} - Failure: Validation error (Blank label)")
    void update_BlankLabel_ReturnsBadRequest() throws Exception {
        // Given: label is blank
        TicketTypeRequestDto invalidRequest = new TicketTypeRequestDto("", new BigDecimal("1.0"), false);

        mockMvc.perform(patch("/api/v1/ticket-types/{id}", TICKET_TYPE_ID)
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("GET /api/v1/ticket-types/my - Success: Get list for current organization")
    void getMyTicketTypes_Success() throws Exception {
        // Given
        TicketTypeResponseDto response = new TicketTypeResponseDto(TICKET_TYPE_ID, "Adult", new BigDecimal("1.00"), true);
        when(ticketTypeService.getTicketTypesByOrganization(any(AppUser.class))).thenReturn(List.of(response));

        // When & Then
        mockMvc.perform(get("/api/v1/ticket-types/my")
                        .with(user(adminDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].label").value("Adult"))
                .andExpect(jsonPath("$[0].isDefault").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/ticket-types/my - Failure: Unauthorized/Forbidden for anonymous")
    void getMyTicketTypes_Anonymous_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/ticket-types/my"))
                .andDo(print())
                // Change status to isForbidden() if your security config returns 403
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/ticket-types/my - Failure: Forbidden for regular USER")
    void getMyTicketTypes_RegularUser_ReturnsForbidden() throws Exception {
        // When & Then: Request as ROLE_USER
        mockMvc.perform(get("/api/v1/ticket-types/my")
                        .with(user(regularDetails)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/ticket-types/my - Success: Returns empty list if no types exist")
    void getMyTicketTypes_EmptyList_ReturnsOk() throws Exception {
        // Given: Service returns an empty collection
        when(ticketTypeService.getTicketTypesByOrganization(any(AppUser.class))).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/ticket-types/my")
                        .with(user(adminDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }


    @Test
    @DisplayName("GET /api/v1/ticket-types/organization/{orgId} - Success: Public access")
    void getByOrgId_Success() throws Exception {
        // Given
        TicketTypeResponseDto response = new TicketTypeResponseDto(TICKET_TYPE_ID, "Student", new BigDecimal("0.80"), false);
        when(ticketTypeService.getTicketTypesByOrganizationId(eq(ORG_ID))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/ticket-types/organization/{orgId}", ORG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("Student"));
    }

    @Test
    @DisplayName("GET /api/v1/ticket-types/organization/{orgId} - Success: Returns empty list if no types found")
    void getByOrgId_EmptyList_ReturnsOk() throws Exception {
        // Given
        when(ticketTypeService.getTicketTypesByOrganizationId(eq(ORG_ID))).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/ticket-types/organization/{orgId}", ORG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/ticket-types/organization/{orgId} - Failure: Invalid organization ID format")
    void getByOrgId_InvalidId_ReturnsBadRequest() throws Exception {
        // When & Then: Passing string instead of UUID
        mockMvc.perform(get("/api/v1/ticket-types/organization/{orgId}", "not-a-uuid"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/ticket-types/organization/{orgId} - Success: Works for authenticated user")
    void getByOrgId_Authenticated_Success() throws Exception {
        // Given
        TicketTypeResponseDto response = new TicketTypeResponseDto(TICKET_TYPE_ID, "VIP", new BigDecimal("2.0"), false);
        when(ticketTypeService.getTicketTypesByOrganizationId(eq(ORG_ID))).thenReturn(List.of(response));

        // When & Then: Even with ROLE_USER context
        mockMvc.perform(get("/api/v1/ticket-types/organization/{orgId}", ORG_ID)
                        .with(user(regularDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("VIP"));
    }

    @Test
    @DisplayName("DELETE /api/v1/ticket-types/{id} - Success")
    void delete_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/ticket-types/{id}", TICKET_TYPE_ID)
                        .with(user(adminDetails))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(ticketTypeService, times(1)).deleteTicketType(eq(TICKET_TYPE_ID), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/ticket-types/{id} - Failure: Forbidden for regular USER")
    void delete_RegularUser_ReturnsForbidden() throws Exception {
        // When & Then: User with ROLE_USER attempts to delete
        mockMvc.perform(delete("/api/v1/ticket-types/{id}", TICKET_TYPE_ID)
                        .with(user(regularDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());

        // Verify service was never called due to security restriction
        verify(ticketTypeService, never()).deleteTicketType(any(), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/ticket-types/{id} - Failure: Unauthorized for anonymous")
    void delete_Anonymous_ReturnsUnauthorized() throws Exception {
        // When & Then: No authentication context
        mockMvc.perform(delete("/api/v1/ticket-types/{id}", TICKET_TYPE_ID)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/v1/ticket-types/{id} - Failure: Ticket type not found")
    void delete_NotFound_Returns404() throws Exception {
        // Given: Service throws exception if ID doesn't exist
        doThrow(new jakarta.persistence.EntityNotFoundException("Ticket type not found"))
                .when(ticketTypeService).deleteTicketType(eq(TICKET_TYPE_ID), any(AppUser.class));

        // When & Then
        mockMvc.perform(delete("/api/v1/ticket-types/{id}", TICKET_TYPE_ID)
                        .with(user(adminDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/ticket-types/{id} - Failure: Invalid UUID format")
    void delete_InvalidUuid_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/ticket-types/{id}", "not-a-uuid")
                        .with(user(adminDetails))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

}
