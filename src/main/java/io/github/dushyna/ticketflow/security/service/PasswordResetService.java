package io.github.dushyna.ticketflow.security.service;

import io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException;
import io.github.dushyna.ticketflow.security.entities.PasswordResetToken;
import io.github.dushyna.ticketflow.security.repository.PasswordResetTokenRepository;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetToken createToken(AppUser user) {
        PasswordResetToken token = new PasswordResetToken(
                UUID.randomUUID().toString(),
                user,
                LocalDateTime.now().plusMinutes(30)
        );

        PasswordResetToken saved = tokenRepository.save(token);

        log.info("PASSWORD RESET TOKEN CREATED: {}", saved.getToken());

        return saved;
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RestApiException(HttpStatus.BAD_REQUEST, "Invalid token"));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RestApiException(HttpStatus.BAD_REQUEST, "Token expired");
        }

        AppUser user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.delete(resetToken);
    }

    public void validateToken(String tokenValue) {
        PasswordResetToken token = tokenRepository
                .findByToken(tokenValue)
                .orElseThrow(() ->
                        new RestApiException(HttpStatus.BAD_REQUEST, "Invalid reset token")
                );

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RestApiException(HttpStatus.BAD_REQUEST, "Reset token has expired");
        }
    }
}