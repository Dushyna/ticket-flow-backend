package io.github.dushyna.ticketflow.security.controller;

import io.github.dushyna.ticketflow.security.controller.api.AuthApi;
import io.github.dushyna.ticketflow.security.dto.LoginRequest;
import io.github.dushyna.ticketflow.security.dto.request.ForgotPasswordRequestDto;
import io.github.dushyna.ticketflow.security.dto.request.ResetPasswordRequestDto;
import io.github.dushyna.ticketflow.security.service.AuthService;
import io.github.dushyna.ticketflow.security.service.CookieService;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.service.interfaces.UserService;
import io.github.dushyna.ticketflow.user.utils.AppUserMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.WebUtils;

import static io.github.dushyna.ticketflow.security.constants.Constants.ACCESS_TOKEN_COOKIE;
import static io.github.dushyna.ticketflow.security.constants.Constants.REFRESH_TOKEN_COOKIE;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;
    private final CookieService cookieService;
    private final UserService userService;
    private final AppUserMapper userMapper;

    @Override
    public UserResponseDto login(LoginRequest loginRequest, HttpServletResponse response) {
        authService.login(loginRequest, response);

        AppUser user = userService.getByEmailOrThrow(loginRequest.email());
        return userMapper.mapEntityToResponseDto(user);
    }

    @Override
    public void refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie refreshCookie = WebUtils.getCookie(request, REFRESH_TOKEN_COOKIE);
        if (refreshCookie == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token missing");
        }

        String newAccessToken = authService.refreshAccessToken(refreshCookie.getValue());

        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.generateAccessTokenCookie(newAccessToken));
    }

    @Override
    public void logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.generateLogoutCookie(ACCESS_TOKEN_COOKIE));
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.generateLogoutCookie(REFRESH_TOKEN_COOKIE));
        SecurityContextHolder.clearContext();
    }

    @Override
    public UserResponseDto getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        AppUser user = userService.getByEmailOrThrow(authentication.getName());
        return userMapper.mapEntityToResponseDto(user);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequestDto request) {
        authService.forgotPassword(request.email());
    }

    @Override
    public void validateResetToken(String token) {
        authService.validateResetToken(token);
    }

    @Override
    public void resetPassword(ResetPasswordRequestDto request) {
        authService.resetPassword(request.token(), request.newPassword());
    }
}
