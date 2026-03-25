package io.github.dushyna.ticketflow.security.controller;

import io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException;
import io.github.dushyna.ticketflow.security.controller.api.AuthApi;
import io.github.dushyna.ticketflow.security.dto.LoginRequest;
import io.github.dushyna.ticketflow.security.dto.request.ForgotPasswordRequestDto;
import io.github.dushyna.ticketflow.security.dto.request.ResetPasswordRequestDto;
import io.github.dushyna.ticketflow.security.dto.response.TokenResponseDto;
import io.github.dushyna.ticketflow.security.service.AuthService;
import io.github.dushyna.ticketflow.security.service.CookieService;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.service.interfaces.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Arrays;

import static io.github.dushyna.ticketflow.security.constants.Constants.ACCESS_TOKEN_COOKIE;
import static io.github.dushyna.ticketflow.security.constants.Constants.REFRESH_TOKEN_COOKIE;

/**
 * Controller that receives authorization http requests
 */
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService service;
    private final CookieService cookieService;
    private final UserService userService;


    @Override
    public TokenResponseDto login(LoginRequest loginRequest, HttpServletResponse response) {
        final TokenResponseDto tokens = service.login(loginRequest);

        final Cookie accessCookie = cookieService.generateAccessTokenCookie(tokens.getAccessToken());
        final Cookie refreshCookie = cookieService.generateRefreshTokenCookie(tokens.getRefreshToken());

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);

        return tokens;
    }

    @Override
    public TokenResponseDto refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {

        String curRefreshToken = extractRefreshTokenFromCookies(request);
        String newAccessToken = service.refreshAccessToken(curRefreshToken);
        Cookie accessCookie = cookieService.generateAccessTokenCookie(newAccessToken);
        response.addCookie(accessCookie);

        return new TokenResponseDto(newAccessToken, curRefreshToken);

    }

    private String extractRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new RestApiException(
                    HttpStatus.UNAUTHORIZED,"No cookies found!"
            );
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> REFRESH_TOKEN_COOKIE.equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() -> new RestApiException(
                        HttpStatus.UNAUTHORIZED, "Refresh token not found!"
                ));
    }

    @Override
    public TokenResponseDto logout(HttpServletResponse response) {
        final Cookie accessCookie = cookieService.generateLogoutCookie(ACCESS_TOKEN_COOKIE);
        final Cookie refreshCookie = cookieService.generateLogoutCookie(REFRESH_TOKEN_COOKIE);
        SecurityContextHolder.clearContext();

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);

        return new TokenResponseDto(null, null);
    }

    @Override
    public UserResponseDto getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        String email;
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            email = oauth2User.getAttribute("email");
        } else {
            email = authentication.getName();
        }

        AppUser appUser = userService.getByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return new UserResponseDto(
                appUser.getId().toString(),
                appUser.getEmail(),
                appUser.getFirstName(),
                appUser.getLastName(),
                appUser.getBirthDate(),
                appUser.getPhone(),
                appUser.getRole().name(),
                appUser.getConfirmationStatus(),
                appUser.getOrganization() != null
                        ? appUser.getOrganization().getId().toString()
                        : null
        );
    }

    @Override
    public void forgotPassword(ForgotPasswordRequestDto request) {
        service.forgotPassword(request.email());
    }

    @Override
    public void validateResetToken(String token) {
        service.validateResetToken(token);
    }

    @Override
    public void resetPassword(ResetPasswordRequestDto request) {
        service.resetPassword(request.token(), request.newPassword());
    }
}
