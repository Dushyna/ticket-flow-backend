package io.github.dushyna.ticketflow.security.handler;

import io.github.dushyna.ticketflow.security.service.CookieService;
import io.github.dushyna.ticketflow.security.service.JwtTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenService jwtTokenService;
    private final CookieService cookieService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        String email;
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            email = oauth2User.getAttribute("email");
        } else {
            email = authentication.getName();
        }

        String accessToken = jwtTokenService.generateAccessToken(email);
        String refreshToken = jwtTokenService.generateRefreshToken(email);

        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE,
                cookieService.generateAccessTokenCookie(accessToken));
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE,
                cookieService.generateRefreshTokenCookie(refreshToken));

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
