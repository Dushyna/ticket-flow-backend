package io.github.dushyna.ticketflow.security.service;

import io.github.dushyna.ticketflow.security.constants.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class CookieService {

    @Value("${jwt.at.live-in-min}")
    private int accessTokenLiveInMinutes;
    @Value("${jwt.rt.live-in-min}")
    private int refreshTokenLiveInMinutes;

    public String generateAccessTokenCookie(String token) {
        return createCookie(Constants.ACCESS_TOKEN_COOKIE, token, accessTokenLiveInMinutes);
    }

    public String generateRefreshTokenCookie(String token) {
        return createCookie(Constants.REFRESH_TOKEN_COOKIE, token, refreshTokenLiveInMinutes);
    }

    public String generateLogoutCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build()
                .toString();
    }

    private String createCookie(String name, String value, int minutes) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofMinutes(minutes))
                .sameSite("Lax")
                .build()
                .toString();
    }
}
