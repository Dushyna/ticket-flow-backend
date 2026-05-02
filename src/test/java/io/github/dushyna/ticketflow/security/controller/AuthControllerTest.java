package io.github.dushyna.ticketflow.security.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dushyna.ticketflow.security.dto.LoginRequest;
import io.github.dushyna.ticketflow.security.dto.request.ForgotPasswordRequestDto;
import io.github.dushyna.ticketflow.security.dto.request.ResetPasswordRequestDto;
import io.github.dushyna.ticketflow.security.service.AuthService;
import io.github.dushyna.ticketflow.security.service.CookieService;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.entity.ConfirmationStatus;
import io.github.dushyna.ticketflow.user.service.interfaces.UserService;
import io.github.dushyna.ticketflow.user.utils.AppUserMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static io.github.dushyna.ticketflow.security.constants.Constants.ACCESS_TOKEN_COOKIE;
import static io.github.dushyna.ticketflow.security.constants.Constants.REFRESH_TOKEN_COOKIE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CookieService cookieService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AppUserMapper userMapper;

    @Test
    @DisplayName("POST /api/v1/auth/login - Success: Returns user and sets cookies")
    void login_Success() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("test@test.com", "password123");
        AppUser mockUser = new AppUser();
        UserResponseDto responseDto = new UserResponseDto(
                "id-1", "test@test.com", "Name", "Surname", null, null, "ROLE_USER", ConfirmationStatus.CONFIRMED, null
        );

        // Simulate successful login process
        doNothing().when(authService).login(any(), any());
        when(userService.getByEmailOrThrow(anyString())).thenReturn(mockUser);
        when(userMapper.mapEntityToResponseDto(any())).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Failure: Validation error (empty email)")
    void login_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given: Empty email violates @NotBlank in LoginRequest
        LoginRequest invalidRequest = new LoginRequest("", "password");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh-token - Success")
    void refreshAccessToken_Success() throws Exception {
        Cookie refreshCookie = new Cookie(REFRESH_TOKEN_COOKIE, "valid-refresh-token");

        when(authService.refreshAccessToken("valid-refresh-token")).thenReturn("new-access-token");
        when(cookieService.generateAccessTokenCookie("new-access-token"))
                .thenReturn("access_token=new-access-token; Path=/; HttpOnly");

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .with(csrf())
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(ACCESS_TOKEN_COOKIE))
                .andExpect(cookie().value(ACCESS_TOKEN_COOKIE, "new-access-token"))
                .andExpect(cookie().httpOnly(ACCESS_TOKEN_COOKIE, true));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh-token - Failure: Refresh token missing")
    void refreshAccessToken_NoCookie_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token missing"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - Success: Clears cookies")
    void logout_Success() throws Exception {
        // Given
        when(cookieService.generateLogoutCookie(ACCESS_TOKEN_COOKIE))
                .thenReturn("access_token=; Path=/; Max-Age=0");
        when(cookieService.generateLogoutCookie(REFRESH_TOKEN_COOKIE))
                .thenReturn("refresh_token=; Path=/; Max-Age=0");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(cookie().exists(ACCESS_TOKEN_COOKIE))
                .andExpect(cookie().maxAge(ACCESS_TOKEN_COOKIE, 0))
                .andExpect(cookie().exists(REFRESH_TOKEN_COOKIE))
                .andExpect(cookie().maxAge(REFRESH_TOKEN_COOKIE, 0));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - Edge Case: Logout without being authenticated")
    void logout_AnonymousUser_ReturnsOkAndClearsCookies() throws Exception {
        // Given
        // Even if the user is not logged in, service should still generate logout strings
        String logoutAccess = "access_token=; Max-Age=0; Path=/";
        String logoutRefresh = "refresh_token=; Max-Age=0; Path=/";

        when(cookieService.generateLogoutCookie(ACCESS_TOKEN_COOKIE)).thenReturn(logoutAccess);
        when(cookieService.generateLogoutCookie(REFRESH_TOKEN_COOKIE)).thenReturn(logoutRefresh);

        // When & Then
        // Request without any authentication context or cookies
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                // Ensure the system still attempts to clear cookies for the client
                .andExpect(cookie().maxAge(ACCESS_TOKEN_COOKIE, 0))
                .andExpect(cookie().maxAge(REFRESH_TOKEN_COOKIE, 0));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Success: Returns authenticated user details")
    void getCurrentUser_Success() throws Exception {
        // Given
        String email = "homer@simpsons.com";
        AppUser mockUser = new AppUser();
        UserResponseDto responseDto = new UserResponseDto(
                "id-123", email, "Homer", "Simpson", null, null, "ROLE_USER", ConfirmationStatus.CONFIRMED, null
        );

        // Mocking service and mapper
        when(userService.getByEmailOrThrow(email)).thenReturn(mockUser);
        when(userMapper.mapEntityToResponseDto(mockUser)).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/v1/auth/me")
                        // Simulate authenticated state with user principal
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(email)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.firstName").value("Homer"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Failure: Returns 401 when authentication is null")
    void getCurrentUser_NullAuthentication_ReturnsUnauthorized() throws Exception {
        // When & Then
        // Request without any .with(user()) or credentials
        mockMvc.perform(get("/api/v1/auth/me"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Failure: Returns 401 when authentication exists but not authenticated")
    void getCurrentUser_NotAuthenticated_ReturnsUnauthorized() throws Exception {
        // Given: Manually creating an unauthenticated principal
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken unauth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("homer", "password");
        // Token is created but .setAuthenticated(true) is not called

        // When & Then
        mockMvc.perform(get("/api/v1/auth/me")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(unauth)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Edge Case: User in token does not exist in DB")
    void getCurrentUser_UserNotFoundInDb_ReturnsNotFound() throws Exception {
        // Given
        String email = "ghost@test.com";

        // Simulating the scenario where JWT is valid but user record was deleted
        when(userService.getByEmailOrThrow(email))
                .thenThrow(new io.github.dushyna.ticketflow.user.exception.UserNotFoundException());

        // When & Then
        mockMvc.perform(get("/api/v1/auth/me")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(email)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password - Success")
    void forgotPassword_Success() throws Exception {
        // Given
        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("test@example.com");
        doNothing().when(authService).forgotPassword(request.email());

        // When & Then
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password - Failure: Invalid email format")
    void forgotPassword_InvalidEmail_ReturnsBadRequest() throws Exception {
        // Given: Invalid email violates @Email or @NotBlank in DTO
        ForgotPasswordRequestDto invalidRequest = new ForgotPasswordRequestDto("not-an-email");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/auth/reset-password/validate - Success")
    void validateResetToken_Success() throws Exception {
        // Given
        String validToken = "valid-uuid-token";
        doNothing().when(authService).validateResetToken(validToken);

        // When & Then
        mockMvc.perform(get("/api/v1/auth/reset-password/validate")
                        .param("token", validToken)) // Testing @RequestParam
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/auth/reset-password/validate - Failure: Invalid or expired token")
    void validateResetToken_InvalidToken_ReturnsBadRequest() throws Exception {
        // Given
        String invalidToken = "expired-token";
        // Mocking a custom exception from your service
        doThrow(new io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Token expired"))
                .when(authService).validateResetToken(invalidToken);

        // When & Then
        mockMvc.perform(get("/api/v1/auth/reset-password/validate")
                        .param("token", invalidToken))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password - Failure: Password too short")
    void resetPassword_ShortPassword_ReturnsBadRequest() throws Exception {
        // Given: Password is only 4 characters, but @Size(min = 8) is required
        ResetPasswordRequestDto invalidRequest = new ResetPasswordRequestDto("valid-token", "1234");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password - Success")
    void resetPassword_Success() throws Exception {
        ResetPasswordRequestDto request = new ResetPasswordRequestDto("valid-token", "new-secure-password");
        doNothing().when(authService).resetPassword(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

}
