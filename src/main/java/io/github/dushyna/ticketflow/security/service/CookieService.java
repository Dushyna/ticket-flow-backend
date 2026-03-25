package io.github.dushyna.ticketflow.security.service;

import io.github.dushyna.ticketflow.security.constants.Constants;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CookieService {

    @Value("${jwt.at.live-in-min}")
    private int accessTokenLiveInMinutes;
    @Value("${jwt.rt.live-in-min}")
    private int refreshTokenLiveInMinutes;

    public Cookie generateLogoutCookie(final String cookieName) {
        final Cookie cookie = new Cookie(cookieName, null);
        configureCommonCookieSettings(cookie);
        cookie.setMaxAge(0);
        return cookie;
    }

    public Cookie generateAccessTokenCookie(final String accessToken) {
        final Cookie cookie = new Cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken);
        configureCommonCookieSettings(cookie);
        cookie.setMaxAge(convertMinutesToSeconds(accessTokenLiveInMinutes));
        return cookie;
    }

    public Cookie generateRefreshTokenCookie(final String refreshToken) {
        final Cookie cookie = new Cookie(Constants.REFRESH_TOKEN_COOKIE, refreshToken);
        configureCommonCookieSettings(cookie);
        cookie.setMaxAge(convertMinutesToSeconds(refreshTokenLiveInMinutes));
        return cookie;
    }

    private void configureCommonCookieSettings(final Cookie cookie) {
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
    }

    private int convertMinutesToSeconds(int minutes) {
        return minutes * 60;
    }
}
