package io.github.dushyna.ticketflow.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Unit tests for JwtTokenService")
class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;


    @BeforeEach
    void setUp() {
         String accessSecret = "dGhpc2lzYXZlcnlzZWN1cmVhY2Nlc3N0b2tlbnNlY3JldGtleTI1Ng==";
         String refreshSecret = "dGhpc2lzYXZlcnlzZWN1cmVpbmZ1c2lvbnJlZnJlc2h0b2tlbnNlY3JldGtleTI1Ng==";

        jwtTokenService = new JwtTokenService(accessSecret, refreshSecret);

        ReflectionTestUtils.setField(jwtTokenService, "accessTokenLiveInMinutes", 15);
        ReflectionTestUtils.setField(jwtTokenService, "refreshTokenLiveInMinutes", 10080);
    }

    @Test
    @DisplayName("Success: Generate and parse valid Access Token")
    void generateAndParse_AccessToken_Success() {
        String email = "user@test.com";

        String token = jwtTokenService.generateAccessToken(email);
        String extractedEmail = jwtTokenService.getUsernameFromToken(token, JwtTokenService.TokenType.ACCESS);

        assertThat(token).isNotBlank();
        assertThat(extractedEmail).isEqualTo(email);
        assertThat(jwtTokenService.validateToken(token, JwtTokenService.TokenType.ACCESS)).isTrue();
    }

    @Test
    @DisplayName("Error: Should return false for invalid signature")
    void validateToken_InvalidSignature_ReturnsFalse() {
        String email = "user@test.com";
        String token = jwtTokenService.generateAccessToken(email);

        boolean isValid = jwtTokenService.validateToken(token, JwtTokenService.TokenType.REFRESH);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Error: Should return false for expired token")
    void validateToken_ExpiredToken_ReturnsFalse() {
        ReflectionTestUtils.setField(jwtTokenService, "accessTokenLiveInMinutes", 0);

        String token = jwtTokenService.generateAccessToken("user@test.com");

        boolean isValid = jwtTokenService.validateToken(token, JwtTokenService.TokenType.ACCESS);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Error: Constructor should throw exception if secrets are too short")
    void constructor_ShortSecrets_ThrowsException() {
        assertThatThrownBy(() -> new JwtTokenService("short", "also-short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 32 characters long");
    }

    @Test
    @DisplayName("Error: getUsernameFromToken should throw Exception for malformed token")
    void getUsername_MalformedToken_ThrowsException() {
        assertThatThrownBy(() ->
                jwtTokenService.getUsernameFromToken("invalid.token.here", JwtTokenService.TokenType.ACCESS))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Edge Case: Should fail validation when token string is manipulated")
    void validateToken_ManipulatedToken_ReturnsFalse() {
        String token = jwtTokenService.generateAccessToken("user@test.com");

        String manipulatedToken = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        boolean isValid = jwtTokenService.validateToken(manipulatedToken, JwtTokenService.TokenType.ACCESS);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Edge Case: Should extract username even if token is 1 second before expiration")
    void getUsername_JustBeforeExpiration_Success() {
        ReflectionTestUtils.setField(jwtTokenService, "accessTokenLiveInMinutes", 1);

        String email = "edge@test.com";
        String token = jwtTokenService.generateAccessToken(email);

        // When
        String result = jwtTokenService.getUsernameFromToken(token, JwtTokenService.TokenType.ACCESS);

        // Then
        assertThat(result).isEqualTo(email);
    }

}
