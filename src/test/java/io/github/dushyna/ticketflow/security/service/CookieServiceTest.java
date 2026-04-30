package io.github.dushyna.ticketflow.security.service;

import io.github.dushyna.ticketflow.security.constants.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for CookieService")
class CookieServiceTest {

    private CookieService cookieService;

    @BeforeEach
    void setUp() {
        cookieService = new CookieService();
        ReflectionTestUtils.setField(cookieService, "accessTokenLiveInMinutes", 15);
        ReflectionTestUtils.setField(cookieService, "refreshTokenLiveInMinutes", 10080);
    }

    @Test
    @DisplayName("Should generate valid Access Token cookie")
    void generateAccessTokenCookie_Success() {
        String token = "test-access-token";
        String cookie = cookieService.generateAccessTokenCookie(token);

        assertThat(cookie)
                .contains(Constants.ACCESS_TOKEN_COOKIE + "=" + token)
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax")
                .contains("Max-Age=900");
    }

    @Test
    @DisplayName("Should generate valid Refresh Token cookie")
    void generateRefreshTokenCookie_Success() {
        String token = "test-refresh-token";
        String cookie = cookieService.generateRefreshTokenCookie(token);

        assertThat(cookie)
                .contains(Constants.REFRESH_TOKEN_COOKIE + "=" + token)
                .contains("Max-Age=604800");
    }

    @Test
    @DisplayName("Should generate valid Logout cookie with zero max-age")
    void generateLogoutCookie_Success() {
        String cookieName = Constants.ACCESS_TOKEN_COOKIE;
        String cookie = cookieService.generateLogoutCookie(cookieName);

        assertThat(cookie)
                .contains(cookieName + "=")
                .contains("Max-Age=0")
                .contains("HttpOnly");
    }

    @Test
    @DisplayName("Should handle empty token value")
    void generateCookie_WithEmptyToken() {
        String cookie = cookieService.generateAccessTokenCookie("");

        assertThat(cookie).contains(Constants.ACCESS_TOKEN_COOKIE + "=");
    }

    @Test
    @DisplayName("Should generate cookie with zero Max-Age if minutes are zero")
    void generateCookie_WithZeroMinutes() {
        ReflectionTestUtils.setField(cookieService, "accessTokenLiveInMinutes", 0);

        String cookie = cookieService.generateAccessTokenCookie("token");

        assertThat(cookie).contains("Max-Age=0");
    }

}
