package io.github.dushyna.ticketflow.security.service;

import io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException;
import io.github.dushyna.ticketflow.mail.EmailService;
import io.github.dushyna.ticketflow.security.dto.LoginRequest;
import io.github.dushyna.ticketflow.security.entities.PasswordResetToken;
import io.github.dushyna.ticketflow.security.repository.PasswordResetTokenRepository;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for AuthService")
class AuthServiceTest {

    @Mock private JwtTokenService jwtTokenService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository resetTokenRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordResetService passwordResetService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CookieService cookieService;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("Login functionality tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully, generate tokens and set cookies")
        void login_Success() {
            // Given
            String email = "admin@ticketflow.com";
            LoginRequest request = new LoginRequest(email, "strongPassword");
            HttpServletResponse response = mock(HttpServletResponse.class);
            UserDetails userDetails = mock(UserDetails.class);
            Authentication auth = mock(Authentication.class);

            AppUser user = new AppUser();
            user.setEmail(email);
            user.setFailedAttempts(3);
            given(userRepository.findByEmailIgnoreCase(email)).willReturn(Optional.of(user));

            given(userDetailsService.loadUserByUsername(email)).willReturn(userDetails);
            given(authenticationManager.authenticate(any())).willReturn(auth);
            given(jwtTokenService.generateAccessToken(email)).willReturn("access-jwt");
            given(jwtTokenService.generateRefreshToken(email)).willReturn("refresh-jwt");
            given(cookieService.generateAccessTokenCookie("access-jwt")).willReturn("access-cookie-header");
            given(cookieService.generateRefreshTokenCookie("refresh-jwt")).willReturn("refresh-cookie-header");

            // When
            authService.login(request, response);

            // Then
            assertThat(user.getFailedAttempts()).isEqualTo(0);
            verify(response, times(2)).addHeader(eq("Set-Cookie"), anyString());
            verify(jwtTokenService).generateAccessToken(email);
            verify(jwtTokenService).generateRefreshToken(email);
            verify(userRepository, atLeastOnce()).save(user);
        }

        @Test
        @DisplayName("Should throw Unauthorized when credentials are invalid")
        void login_ThrowsBadCredentials() {
            // Given
            String email = "a@b.com";
            AppUser user = new AppUser();
            user.setEmail(email);
            given(userRepository.findByEmailIgnoreCase(email)).willReturn(Optional.of(user));
            given(userDetailsService.loadUserByUsername(any())).willReturn(mock(UserDetails.class));
            given(authenticationManager.authenticate(any())).willThrow(new BadCredentialsException("Invalid"));

            // When & Then
            assertThatThrownBy(() -> authService.login(new LoginRequest("a@b.com", "p"), mock(HttpServletResponse.class)))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(ex -> {
                        RestApiException apiEx = (RestApiException) ex;
                        assertThat(apiEx.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                        assertThat(apiEx.getMessage()).isEqualTo("Invalid username or password.");
                    });
            assertThat(user.getFailedAttempts()).isEqualTo(1);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should throw Forbidden when account is locked")
        void login_ThrowsLockedException() {
            // Given
            String email = "locked@example.com";
            AppUser user = new AppUser();
            user.setEmail(email);

            given(userRepository.findByEmailIgnoreCase(email)).willReturn(Optional.of(user));
            given(userDetailsService.loadUserByUsername(email)).willReturn(mock(UserDetails.class));
            given(authenticationManager.authenticate(any())).willThrow(new LockedException("Locked"));

            // When & Then
            assertThatThrownBy(() -> authService.login(new LoginRequest(email, "pass"), mock(HttpServletResponse.class)))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(ex -> {
                        RestApiException apiEx = (RestApiException) ex;
                        assertThat(apiEx.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                        assertThat(apiEx.getMessage()).isEqualTo("User account is locked.");
                    });
        }

        @Test
        @DisplayName("Should throw Forbidden when account is disabled (not confirmed)")
        void login_ThrowsDisabledException() {
            // Given
            String email = "not_confirmed@example.com";
            AppUser user = new AppUser();
            user.setEmail(email);

            given(userRepository.findByEmailIgnoreCase(email)).willReturn(Optional.of(user));
            given(userDetailsService.loadUserByUsername(email)).willReturn(mock(UserDetails.class));
            given(authenticationManager.authenticate(any())).willThrow(new DisabledException("Account disabled"));

            // When & Then
            assertThatThrownBy(() -> authService.login(new LoginRequest(email, "any"), mock(HttpServletResponse.class)))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(ex -> {
                        RestApiException apiEx = (RestApiException) ex;
                        assertThat(apiEx.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                        assertThat(apiEx.getMessage()).isEqualTo("User account is not active. Please confirm your email.");
                    });
        }

        @Test
        @DisplayName("handleFailedLogin: should lock user after 5 attempts")
        void handleFailedLogin_ShouldLockAfter5Attempts() {
            // Given
            String email = "test@test.com";
            AppUser user = new AppUser();
            user.setEmail(email);
            user.setFailedAttempts(4);
            user.setLocked(false);
            given(userRepository.findByEmailIgnoreCase(email)).willReturn(Optional.of(user));
            given(userDetailsService.loadUserByUsername(email)).willReturn(mock(UserDetails.class));
            given(authenticationManager.authenticate(any())).willThrow(new BadCredentialsException("Wrong"));

            // When
            assertThatThrownBy(() -> authService.login(new LoginRequest(email, "wrong"), mock(HttpServletResponse.class)))
                    .isInstanceOf(RestApiException.class);

            // Then
            assertThat(user.getFailedAttempts()).isEqualTo(5);
            assertThat(user.isLocked()).isTrue();
            verify(userRepository).save(user);        }


    }

    @Nested
    @DisplayName("Token refresh tests")
    class RefreshTests {

        @Test
        @DisplayName("Should refresh Access Token successfully with valid Refresh Token")
        void refresh_Success() {
            // Given
            String refresh = "valid-refresh";
            given(jwtTokenService.validateToken(refresh, JwtTokenService.TokenType.REFRESH)).willReturn(true);
            given(jwtTokenService.getUsernameFromToken(refresh, JwtTokenService.TokenType.REFRESH)).willReturn("user");
            given(jwtTokenService.generateAccessToken("user")).willReturn("new-access");

            // When
            String result = authService.refreshAccessToken(refresh);

            // Then
            assertThat(result).isEqualTo("new-access");
        }

        @Test
        @DisplayName("Should throw Unauthorized when refresh token is invalid")
        void refresh_InvalidToken_ThrowsUnauthorized() {
            // Given
            String invalidRefresh = "fake-or-expired-token";
            given(jwtTokenService.validateToken(invalidRefresh, JwtTokenService.TokenType.REFRESH))
                    .willReturn(false);

            // When & Then
            assertThatThrownBy(() -> authService.refreshAccessToken(invalidRefresh))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(ex -> {
                        RestApiException apiEx = (RestApiException) ex;
                        assertThat(apiEx.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                        assertThat(apiEx.getMessage()).isEqualTo("Invalid refresh token");
                    });

            verify(jwtTokenService, never()).getUsernameFromToken(anyString(), any());
            verify(jwtTokenService, never()).generateAccessToken(anyString());
        }

    }

    @Nested
    @DisplayName("Password reset functionality tests")
    class PasswordResetTests {

        @Test
        @DisplayName("Should send reset email if user exists")
        void forgotPassword_UserExists() {
            // Given
            String email = "test@mail.com";
            AppUser user = new AppUser();
            user.setEmail(email);
            PasswordResetToken token = new PasswordResetToken();
            token.setToken("reset-code");

            given(userRepository.findByEmailIgnoreCase(email)).willReturn(Optional.of(user));
            given(passwordResetService.createToken(user)).willReturn(token);

            // When
            authService.forgotPassword(email);

            // Then
            verify(emailService).sendResetPasswordEmail(email, "reset-code");
        }

        @Test
        @DisplayName("forgotPassword: should do nothing if user does not exist")
        void forgotPassword_UserDoesNotExist() {
            // Given
            String email = "nonexistent@example.com";
            given(userRepository.findByEmailIgnoreCase(email)).willReturn(Optional.empty());

            // When
            authService.forgotPassword(email);

            // Then
            verify(userRepository).findByEmailIgnoreCase(email);

            verifyNoInteractions(passwordResetService);
            verifyNoInteractions(emailService);
        }


        @Test
        @DisplayName("Should throw exception when validation fails for expired token")
        void validateToken_Expired() {
            // Given
            String tokenValue = "old-token";
            PasswordResetToken token = new PasswordResetToken();
            token.setExpiresAt(LocalDateTime.now().minusMinutes(10));

            given(resetTokenRepository.findByToken(tokenValue)).willReturn(Optional.of(token));

            // When & Then
            assertThatThrownBy(() -> authService.validateResetToken(tokenValue))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("validateResetToken: should throw Bad Request when token is not found")
        void validateResetToken_NotFound() {
            // Given
            String nonExistentToken = "fake-token-123";
            given(resetTokenRepository.findByToken(nonExistentToken)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authService.validateResetToken(nonExistentToken))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(ex -> {
                        RestApiException apiEx = (RestApiException) ex;
                        assertThat(apiEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(apiEx.getMessage()).isEqualTo("Invalid reset token");
                    });
        }

        @Test
        @DisplayName("validateResetToken: should throw Bad Request when token is expired")
        void validateResetToken_Expired() {
            // Given
            String tokenValue = "expired-token";
            PasswordResetToken expiredToken = new PasswordResetToken();
            expiredToken.setToken(tokenValue);
            expiredToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));

            given(resetTokenRepository.findByToken(tokenValue)).willReturn(Optional.of(expiredToken));

            // When & Then
            assertThatThrownBy(() -> authService.validateResetToken(tokenValue))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(ex -> {
                        RestApiException apiEx = (RestApiException) ex;
                        assertThat(apiEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(apiEx.getMessage()).isEqualTo("Reset token expired");
                    });
        }


        @Test
        @DisplayName("Should encode new password and delete token upon success")
        void resetPassword_Success() {
            // Given
            String tokenValue = "token-123";
            AppUser user = new AppUser();
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);

            given(resetTokenRepository.findByToken(tokenValue)).willReturn(Optional.of(token));
            given(passwordEncoder.encode("newPass")).willReturn("hashedPass");

            // When
            authService.resetPassword(tokenValue, "newPass");

            // Then
            assertThat(user.getPassword()).isEqualTo("hashedPass");
            verify(userRepository).save(user);
            verify(resetTokenRepository).delete(token);
        }
    }

    @Test
    @DisplayName("resetPassword: should throw Bad Request when token is not found")
    void resetPassword_NotFound() {
        // Given
        String nonExistentToken = "missing-token";
        String newPassword = "newPassword123";

        given(resetTokenRepository.findByToken(nonExistentToken)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.resetPassword(nonExistentToken, newPassword))
                .isInstanceOf(RestApiException.class)
                .satisfies(ex -> {
                    RestApiException apiEx = (RestApiException) ex;
                    assertThat(apiEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getMessage()).isEqualTo("Invalid reset token");
                });

        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

}
