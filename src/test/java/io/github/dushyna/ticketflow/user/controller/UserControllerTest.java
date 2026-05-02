package io.github.dushyna.ticketflow.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dushyna.ticketflow.user.dto.request.UpdateUserDetailsDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.entity.ConfirmationStatus;
import io.github.dushyna.ticketflow.user.service.interfaces.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("GET /api/v1/users/me-details - Success")
    void getUserDetails_Success() throws Exception {
        String email = "homer@simpsons.com";
        UserResponseDto responseDto = new UserResponseDto(
                "id-123", email, "Homer", "Simpson", null, null, "ROLE_USER", ConfirmationStatus.CONFIRMED, null
        );

        when(userService.getByEmailOrThrow(any())).thenReturn(new AppUser());
        when(userService.getUserDetails(any())).thenReturn(responseDto);

        mockMvc.perform(get("/api/v1/users/me-details")
                        .with(jwt().jwt(j -> j.subject(email))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    @DisplayName("GET /api/v1/users/me-details - Unauthorized (No Token)")
    void getUserDetails_NoToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me-details"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/users/me-details - User Not Found in DB")
    void getUserDetails_UserNotFound_ReturnsNotFound() throws Exception {
        String email = "ghost@test.com";

        when(userService.getByEmailOrThrow(email))
                .thenThrow(new io.github.dushyna.ticketflow.user.exception.UserNotFoundException());

        mockMvc.perform(get("/api/v1/users/me-details")
                        .with(jwt().jwt(j -> j.subject(email))))
                .andDo(print())
                .andExpect(status().isNotFound()) // Перевіряємо, що GlobalExceptionHandler спрацював
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/v1/users/me-details - Empty Subject in JWT")
    void getUserDetails_EmptySubject_ReturnsError() throws Exception {
        String emptyEmail = "";

        when(userService.getByEmailOrThrow(emptyEmail))
                .thenThrow(new io.github.dushyna.ticketflow.user.exception.UserNotFoundException());

        mockMvc.perform(get("/api/v1/users/me-details")
                        .with(jwt().jwt(j -> j.subject(emptyEmail))))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/users/me-details - Expired or Invalid Token Signature")
    void getUserDetails_InvalidToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me-details")
                        .header("Authorization", "Bearer invalid-token"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }


    @Test
    @DisplayName("PATCH /api/v1/users/update-user - Success")
    void updateUserDetails_Success() throws Exception {
        String email = "homer@simpsons.com";
        UpdateUserDetailsDto requestDto = new UpdateUserDetailsDto("Homer", "Simpson", LocalDate.of(1990, 5, 15), "+123");
        UserResponseDto responseDto = new UserResponseDto("id-1", email, "Homer", "Simpson", null, null, "ROLE_USER", ConfirmationStatus.CONFIRMED, null);

        when(userService.getByEmailOrThrow(any())).thenReturn(new AppUser());
        when(userService.updateUserDetails(any(), any())).thenReturn(responseDto);

        mockMvc.perform(patch("/api/v1/users/update-user")
                        .with(csrf())
                        .with(jwt().jwt(j -> j.subject(email)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Homer"));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/update-user - Failure: Unauthorized (No Token)")
    void updateUserDetails_NoToken_ReturnsUnauthorized() throws Exception {
        UpdateUserDetailsDto requestDto = new UpdateUserDetailsDto("Homer", "Simpson", null, "+123");

        // Request without .with(jwt())
        mockMvc.perform(patch("/api/v1/users/update-user")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /api/v1/users/update-user - Failure: User Not Found")
    void updateUserDetails_UserNotFound_ReturnsNotFound() throws Exception {
        String email = "missing@test.com";
        UpdateUserDetailsDto requestDto = new UpdateUserDetailsDto("Homer", "Simpson", null, "+123");

        // Mocking service to throw exception if user is missing in DB
        when(userService.getByEmailOrThrow(email))
                .thenThrow(new io.github.dushyna.ticketflow.user.exception.UserNotFoundException());

        mockMvc.perform(patch("/api/v1/users/update-user")
                        .with(csrf())
                        .with(jwt().jwt(j -> j.subject(email)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/v1/users/update-user - Edge Case: Partial Update (Null Fields)")
    void updateUserDetails_PartialUpdate_ReturnsUpdatedUser() throws Exception {
        // Case: User sends only one field to update, others are null
        String email = "homer@simpsons.com";
        UpdateUserDetailsDto requestDto = new UpdateUserDetailsDto(null, null, null, "+999999");

        UserResponseDto responseDto = new UserResponseDto(
                "id-123", email, "ExistingName", "ExistingSurname", null, "+999999",
                "ROLE_USER", ConfirmationStatus.CONFIRMED, null
        );

        when(userService.getByEmailOrThrow(email)).thenReturn(new AppUser());
        when(userService.updateUserDetails(any(UpdateUserDetailsDto.class), any())).thenReturn(responseDto);

        mockMvc.perform(patch("/api/v1/users/update-user")
                        .with(csrf())
                        .with(jwt().jwt(j -> j.subject(email)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+999999"))
                .andExpect(jsonPath("$.firstName").value("ExistingName")); // Should remain unchanged in mock
    }

    @Test
    @DisplayName("PATCH /api/v1/users/update-user - Edge Case: Malformed JSON")
    void updateUserDetails_MalformedJson_ReturnsBadRequest() throws Exception {
        String email = "homer@simpsons.com";
        String invalidJson = "{ \"firstName\": \"Homer\", \"birthDate\": \"invalid-date-format\" }";

        // Testing how Jackson and GlobalExceptionHandler handle bad date format
        mockMvc.perform(patch("/api/v1/users/update-user")
                        .with(csrf())
                        .with(jwt().jwt(j -> j.subject(email)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/users/all - Success: Allowed for SUPER_ADMIN")
    void getAll_AdminUser_ReturnsList() throws Exception {
        // Given
        UserResponseDto user1 = new UserResponseDto("id-1", "user1@test.com", "User", "One", null, null, "ROLE_USER", ConfirmationStatus.CONFIRMED, null);
        UserResponseDto user2 = new UserResponseDto("id-2", "admin@test.com", "Admin", "User", null, null, "ROLE_SUPER_ADMIN", ConfirmationStatus.CONFIRMED, null);

        when(userService.getAll()).thenReturn(List.of(user1, user2));

        // When & Then
        mockMvc.perform(get("/api/v1/users/all")
                        // Simulating a JWT with the required role
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value("user1@test.com"))
                .andExpect(jsonPath("$[1].email").value("admin@test.com"));
    }

    @Test
    @DisplayName("GET /api/v1/users/all - Failure: Forbidden for regular USER")
    void getAll_RegularUser_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/all")
                        // Simulating a JWT with insufficient permissions
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/users/all - Failure: Unauthorized for Anonymous")
    void getAll_Anonymous_ReturnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/all"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/users/all - Edge Case: Empty List")
    void getAll_AdminUser_ReturnsEmptyList() throws Exception {
        // Given: Admin is logged in, but there are no users in DB
        when(userService.getAll()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/users/all")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

}
