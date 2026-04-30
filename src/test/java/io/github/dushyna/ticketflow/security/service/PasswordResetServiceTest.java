package io.github.dushyna.ticketflow.security.service;

import io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException;
import io.github.dushyna.ticketflow.security.entities.PasswordResetToken;
import io.github.dushyna.ticketflow.security.repository.PasswordResetTokenRepository;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for PasswordResetService")
class PasswordResetServiceTest {

    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    @DisplayName("createToken: should save and return new token")
    void createToken_Success() {
        // Given
        AppUser user = new AppUser();
        given(tokenRepository.save(any(PasswordResetToken.class))).willAnswer(inv -> inv.getArgument(0));

        // When
        PasswordResetToken created = passwordResetService.createToken(user);

        // Then
        assertThat(created.getToken()).isNotBlank();
        assertThat(created.getUser()).isEqualTo(user);
        assertThat(created.getExpiresAt()).isAfter(LocalDateTime.now());
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("validateToken: should throw exception when token not found")
    void validateToken_NotFound_ThrowsException() {
        // Given
        given(tokenRepository.findByToken("fake")).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> passwordResetService.validateToken("fake"))
                .isInstanceOf(RestApiException.class)
                .satisfies(ex -> {
                    RestApiException apiEx = (RestApiException) ex;
                    assertThat(apiEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getMessage()).isEqualTo("Invalid reset token");
                });
    }

    @Test
    @DisplayName("validateToken: should throw exception when token is expired")
    void validateToken_Expired_ThrowsException() {
        // Given
        PasswordResetToken token = new PasswordResetToken();
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        given(tokenRepository.findByToken("expired")).willReturn(Optional.of(token));

        // When & Then
        assertThatThrownBy(() -> passwordResetService.validateToken("expired"))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("Reset token has expired");
    }

    @Test
    @DisplayName("resetPassword: should update password and delete token")
    void resetPassword_Success() {
        // Given
        String tokenVal = "valid-token";
        String newPass = "new-password";
        AppUser user = new AppUser();
        PasswordResetToken token = new PasswordResetToken(tokenVal, user, LocalDateTime.now().plusMinutes(10));

        given(tokenRepository.findByToken(tokenVal)).willReturn(Optional.of(token));
        given(passwordEncoder.encode(newPass)).willReturn("encoded-password");

        // When
        passwordResetService.resetPassword(tokenVal, newPass);

        // Then
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        verify(userRepository).save(user);
        verify(tokenRepository).delete(token);
    }

    @Test
    @DisplayName("Edge Case: Token should be valid if it expires in 1 second")
    void validateToken_JustBeforeExpiration_Success() {
        // Given
        PasswordResetToken token = new PasswordResetToken();
        token.setExpiresAt(LocalDateTime.now().plusSeconds(1));
        given(tokenRepository.findByToken("soon-expired")).willReturn(Optional.of(token));

        // When & Then
        passwordResetService.validateToken("soon-expired");

        verify(tokenRepository).findByToken("soon-expired");
    }

    @Test
    @DisplayName("Edge Case: Should throw correct exception for empty token string")
    void validateToken_EmptyString_ThrowsException() {
        // Given
        given(tokenRepository.findByToken("")).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> passwordResetService.validateToken(""))
                .isInstanceOf(RestApiException.class)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);
    }

}
