package io.github.dushyna.ticketflow.security;

import io.github.dushyna.ticketflow.base.BaseIT;
import io.github.dushyna.ticketflow.exception.handling.response.ErrorResponseDto;
import io.github.dushyna.ticketflow.mail.EmailService;
import io.github.dushyna.ticketflow.security.constants.Constants;
import io.github.dushyna.ticketflow.security.dto.LoginRequest;
import io.github.dushyna.ticketflow.security.dto.request.ForgotPasswordRequestDto;
import io.github.dushyna.ticketflow.security.dto.request.ResetPasswordRequestDto;
import io.github.dushyna.ticketflow.security.entities.PasswordResetToken;
import io.github.dushyna.ticketflow.security.repository.PasswordResetTokenRepository;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.entity.ConfirmationStatus;
import io.github.dushyna.ticketflow.user.entity.Role;
import io.github.dushyna.ticketflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
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
class AuthIntegrationTest extends BaseIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @MockitoBean private S3Client s3Client;
    @MockitoBean private S3AsyncClient s3AsyncClient;
    @MockitoBean private JavaMailSender javaMailSender;
    @MockitoBean private EmailService emailService;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();

        AppUser user = new AppUser();
        user.setEmail("realuser@test.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setFirstName("Real");
        user.setLastName("User");
        user.setRole(Role.ROLE_USER);
        user.setConfirmationStatus(ConfirmationStatus.CONFIRMED);

        userRepository.save(user);
    }

    @Test
    @DisplayName("Full Integration Login: Should return 200 OK and Set-Cookie headers")
    void login_Success() {
        LoginRequest loginRequest = new LoginRequest("realuser@test.com", "password123");

        HttpHeaders headers = getHeadersWithCsrf();
        HttpEntity<LoginRequest> entity = new HttpEntity<>(loginRequest, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/auth/login",
                org.springframework.http.HttpMethod.POST,
                entity,
                Void.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE)).isNotEmpty();
    }

    @Test
    @DisplayName("Integration Login Error: Should return standard ErrorResponseDto")
    void login_ShouldReturnErrorResponse_WhenCredentialsInvalid() {
        // Given
        LoginRequest loginRequest = new LoginRequest("realuser@test.com", "wrong_password");

        // Prepare headers with CSRF to avoid 403 Forbidden
        HttpEntity<LoginRequest> entity = new HttpEntity<>(loginRequest, getHeadersWithCsrf());

        // When: Use exchange to include headers and handle ErrorResponseDto mapping
        ResponseEntity<ErrorResponseDto> response = restTemplate.exchange(
                "/api/v1/auth/login",
                org.springframework.http.HttpMethod.POST,
                entity,
                ErrorResponseDto.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ErrorResponseDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(401);
        assertThat(body.error()).isEqualTo("Unauthorized");
        assertThat(body.message()).isEqualTo("Invalid username or password.");

        // timestamp mapping will now work correctly thanks to JavaTimeModule in BaseIT
        assertThat(body.timestamp()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Integration Login Error: Should return 403 when account is UNCONFIRMED")
    void login_ShouldReturn403_WhenAccountDisabled() {
        // Given
        AppUser user = userRepository.findByEmailIgnoreCase("realuser@test.com").orElseThrow();
        user.setConfirmationStatus(ConfirmationStatus.UNCONFIRMED);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("realuser@test.com", "password123");

        // Prepare headers with CSRF to prevent 403 before logic execution
        HttpEntity<LoginRequest> entity = new HttpEntity<>(loginRequest, getHeadersWithCsrf());

        // When: Use exchange to ensure proper CSRF handling and ErrorResponseDto mapping
        ResponseEntity<ErrorResponseDto> response = restTemplate.exchange(
                "/api/v1/auth/login",
                org.springframework.http.HttpMethod.POST,
                entity,
                ErrorResponseDto.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody())
                .isNotNull()
                .extracting(ErrorResponseDto::message)
                .asString()
                .isNotBlank();
    }

    @Test
    @DisplayName("Full Integration Login Error: Should return 403 when account is manually locked")
    void login_ShouldReturn403_WhenAccountIsLocked() {
        // 1. Given
        AppUser user = userRepository.findByEmailIgnoreCase("realuser@test.com").orElseThrow();
        user.setLocked(true);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("realuser@test.com", "password123");

        // Prepare entity with CSRF headers to pass security filters
        HttpEntity<LoginRequest> entity = new HttpEntity<>(loginRequest, getHeadersWithCsrf());

        // 2. When - Using exchange for proper ErrorResponseDto deserialization
        ResponseEntity<ErrorResponseDto> response = restTemplate.exchange(
                "/api/v1/auth/login",
                org.springframework.http.HttpMethod.POST,
                entity,
                ErrorResponseDto.class
        );

        // 3. Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(response.getBody())
                .isNotNull()
                .satisfies(body -> {
                    assertThat(body.status()).isEqualTo(403);
                    assertThat(body.message()).isEqualTo("User account is locked.");
                });
    }


    @Test
    @DisplayName("Full Integration Forgot Password: Should create a token in DB")
    void forgotPassword_ShouldCreateTokenInDatabase() {
        // 1. Given
        String email = "realuser@test.com";
        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto(email);

        // Prepare entity with CSRF headers to pass security filters
        HttpEntity<ForgotPasswordRequestDto> entity = new HttpEntity<>(request, getHeadersWithCsrf());

        // 2. When - Use exchange to include CSRF headers for POST request
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/auth/forgot-password",
                org.springframework.http.HttpMethod.POST,
                entity,
                Void.class
        );

        // 3. Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var tokens = tokenRepository.findAll();
        assertThat(tokens).hasSize(1);
        assertThat(tokens.getFirst().getUser().getEmail()).isEqualTo(email);
        assertThat(tokens.getFirst().getToken()).isNotBlank();

        // Verify that the email service was triggered
        verify(emailService, atLeastOnce()).sendResetPasswordEmail(eq(email), anyString());
    }


    @Test
    @DisplayName("Integration Forgot Password: Should return 200 even if user not found (Security)")
    void forgotPassword_ShouldReturn200_WhenUserNotFound() {
        // Given
        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("nonexistent@test.com");

        // Prepare entity with CSRF headers
        HttpEntity<ForgotPasswordRequestDto> entity = new HttpEntity<>(request, getHeadersWithCsrf());

        // When: Using exchange to include headers for the POST request
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/auth/forgot-password",
                org.springframework.http.HttpMethod.POST,
                entity,
                Void.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tokenRepository.findAll()).isEmpty();
    }


    @Test
    @DisplayName("Full Integration Reset Password: Should update user password and delete token")
    void resetPassword_ShouldUpdatePasswordInDatabase() {
        // 1. Given
        AppUser user = userRepository.findByEmailIgnoreCase("realuser@test.com").orElseThrow();
        String oldPasswordHash = user.getPassword();
        String resetTokenValue = "valid-reset-token-123";

        PasswordResetToken token = new PasswordResetToken(
                resetTokenValue,
                user,
                LocalDateTime.now().plusHours(1)
        );
        tokenRepository.save(token);

        String newPassword = "brandNewPassword2024";
        ResetPasswordRequestDto request = new ResetPasswordRequestDto(resetTokenValue, newPassword);

        // Prepare headers with CSRF to avoid 403 Forbidden
        HttpEntity<ResetPasswordRequestDto> entity = new HttpEntity<>(request, getHeadersWithCsrf());

        // 2. When - Use exchange to include CSRF headers for POST request
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/auth/reset-password",
                org.springframework.http.HttpMethod.POST,
                entity,
                Void.class
        );

        // 3. Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        AppUser updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getPassword()).isNotEqualTo(oldPasswordHash);
        assertThat(passwordEncoder.matches(newPassword, updatedUser.getPassword())).isTrue();

        Optional<PasswordResetToken> deletedToken = tokenRepository.findByToken(resetTokenValue);
        assertThat(deletedToken).isEmpty();
    }

    @Test
    @DisplayName("Integration Reset Password Error: Should return validation message in ErrorResponseDto")
    void resetPassword_ShouldReturnErrorResponse_WhenTokenInvalid() {
        // Given
        ResetPasswordRequestDto request = new ResetPasswordRequestDto("fake-token", "newPassword123");

        // Prepare entity with CSRF headers to pass security filters
        HttpEntity<ResetPasswordRequestDto> entity = new HttpEntity<>(request, getHeadersWithCsrf());

        // When: Use exchange for proper CSRF handling and ErrorResponseDto mapping
        ResponseEntity<ErrorResponseDto> response = restTemplate.exchange(
                "/api/v1/auth/reset-password",
                org.springframework.http.HttpMethod.POST,
                entity,
                ErrorResponseDto.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ErrorResponseDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.message()).isEqualTo("Invalid reset token");
        assertThat(body.path()).contains("/api/v1/auth/reset-password");
    }

    @Test
    @DisplayName("Integration Refresh: Should return new Access Token when Refresh Cookie is valid")
    void refresh_ShouldReturnNewAccessToken_WhenTokenIsValid() {
        // 1. Given - Login (using exchange + CSRF)
        LoginRequest loginRequest = new LoginRequest("realuser@test.com", "password123");
        HttpEntity<LoginRequest> loginEntity = new HttpEntity<>(loginRequest, getHeadersWithCsrf());

        ResponseEntity<Void> loginResponse = restTemplate.exchange(
                "/api/v1/auth/login",
                org.springframework.http.HttpMethod.POST,
                loginEntity,
                Void.class
        );

        List<String> setCookieHeaders = loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeaders).isNotNull();

        // Extract Refresh Cookie
        String refreshCookieRaw = setCookieHeaders.stream()
                .filter(header -> header.toLowerCase().contains(Constants.REFRESH_TOKEN_COOKIE.toLowerCase()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Refresh-Token not found"));

        String cleanRefreshCookie = refreshCookieRaw.split(";")[0];

        // 2. Prepare request with BOTH CSRF and Refresh Cookie
        HttpHeaders refreshHeaders = getHeadersWithCsrf();
        refreshHeaders.add(HttpHeaders.COOKIE, cleanRefreshCookie);
        HttpEntity<Void> refreshEntity = new HttpEntity<>(refreshHeaders);

        // 3. When
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/auth/refresh-token",
                org.springframework.http.HttpMethod.POST,
                refreshEntity,
                Void.class
        );

        // 4. Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<String> refreshResultCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(refreshResultCookies)
                .withFailMessage("New Access-Token not found in response")
                .anyMatch(c -> c.toLowerCase().contains(Constants.ACCESS_TOKEN_COOKIE.toLowerCase()));
    }

    @Test
    @DisplayName("Integration Refresh Error: Should return 401 when Refresh Cookie is invalid")
    void refresh_ShouldReturn401_WhenTokenIsInvalid() {
        // 1. Given: Prepare headers with CSRF and add a fake/invalid refresh cookie
        HttpHeaders headers = getHeadersWithCsrf();
        headers.add(HttpHeaders.COOKIE, Constants.REFRESH_TOKEN_COOKIE + "=fake-expired-token");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // 2. When: Use exchange to handle CSRF and ErrorResponseDto mapping
        ResponseEntity<ErrorResponseDto> response = restTemplate.exchange(
                "/api/v1/auth/refresh-token",
                org.springframework.http.HttpMethod.POST,
                entity,
                ErrorResponseDto.class
        );

        // 3. Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // ErrorResponseDto timestamp will be correctly parsed thanks to BaseIT config
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(401);
    }

    @Test
    @DisplayName("Integration Reset Token Error: Should return 400 when token does not exist in DB")
    void validateResetToken_ShouldReturn400_WhenNotFound() {
        // When
        ResponseEntity<ErrorResponseDto> response = restTemplate.getForEntity(
                "/api/v1/auth/reset-password/validate?token=non-existent-uuid",
                ErrorResponseDto.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid reset token");
    }
    @Test
    @DisplayName("Integration Reset Token Error: Should return 400 when token has expired")
    void validateResetToken_ShouldReturn400_WhenExpired() {
        // 1. Given
        AppUser user = userRepository.findByEmailIgnoreCase("realuser@test.com").orElseThrow();
        PasswordResetToken expiredToken = new PasswordResetToken(
                "expired-token-123",
                user,
                LocalDateTime.now().minusMinutes(10)
        );
        tokenRepository.save(expiredToken);

        // 2. When
        ResponseEntity<ErrorResponseDto> response = restTemplate.getForEntity(
                "/api/v1/auth/reset-password/validate?token=expired-token-123",
                ErrorResponseDto.class
        );

        // 3. Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Reset token expired");
    }


}