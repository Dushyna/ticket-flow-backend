package io.github.dushyna.ticketflow.security.service;

import io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException;
import io.github.dushyna.ticketflow.mail.EmailService;
import io.github.dushyna.ticketflow.security.dto.LoginRequest;
import io.github.dushyna.ticketflow.security.entities.PasswordResetToken;
import io.github.dushyna.ticketflow.security.repository.PasswordResetTokenRepository;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final JwtTokenService jwtTokenService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailService emailService;
    private final PasswordResetService passwordResetService;
    private final PasswordEncoder passwordEncoder;
    private final CookieService cookieService;

    public void login(LoginRequest loginRequest, HttpServletResponse response) {
        String userEmail = loginRequest.email();
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userDetails, loginRequest.password())
            );
        } catch (DisabledException ex) {
            log.warn("Login attempt for inactive account: {}", userEmail, ex);
            throw new RestApiException(
                    HttpStatus.FORBIDDEN,
                    "User account is not active. Please confirm your email."
            );
        } catch (LockedException ex) {
            log.warn("Login attempt for locked account: {}", userEmail, ex);
            throw new RestApiException(
                    HttpStatus.FORBIDDEN,
                    "User account is locked."
            );
        } catch (BadCredentialsException ex) {
            log.warn("Bad credentials for user: {}", userEmail, ex);
            throw new RestApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid username or password."
            );
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtTokenService.generateAccessToken(userEmail);
        String refreshToken = jwtTokenService.generateRefreshToken(userEmail);
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookieService.generateAccessTokenCookie(accessToken));
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookieService.generateRefreshTokenCookie(refreshToken));
    }

    public String refreshAccessToken(String refreshToken) {
        if (jwtTokenService.validateToken(refreshToken, JwtTokenService.TokenType.REFRESH)) {
            String username = jwtTokenService.getUsernameFromToken(refreshToken, JwtTokenService.TokenType.REFRESH);
            return jwtTokenService.generateAccessToken(username);
        }
        throw new RestApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
    }

    public void forgotPassword(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {

            PasswordResetToken token = passwordResetService.createToken(user);

            emailService.sendResetPasswordEmail(
                    user.getEmail(),
                    token.getToken()
            );
        });
    }

    public void validateResetToken(String token) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RestApiException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid reset token"
                ));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RestApiException(
                    HttpStatus.BAD_REQUEST,
                    "Reset token expired"
            );
        }
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RestApiException(HttpStatus.BAD_REQUEST, "Invalid reset token"));
        AppUser user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetTokenRepository.delete(resetToken);
    }
}