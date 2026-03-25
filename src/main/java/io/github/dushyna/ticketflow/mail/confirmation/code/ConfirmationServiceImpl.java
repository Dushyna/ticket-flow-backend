package io.github.dushyna.ticketflow.mail.confirmation.code;

import io.github.dushyna.ticketflow.mail.confirmation.code.interfaces.ConfirmationCodeRepository;
import io.github.dushyna.ticketflow.mail.confirmation.code.interfaces.ConfirmationService;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.exception.UserConfirmationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConfirmationServiceImpl implements ConfirmationService {

    @Value("${confirmation.expiration.days:5}")
    private int confirmationDays;

    private final ConfirmationCodeRepository repository;

    @Override
    public String generateConfirmationCode(final AppUser user) {
        return generateConfirmation(user).getId().toString();
    }

    @Override
    public ConfirmationCode generateConfirmation(final AppUser user) {
        final Instant expiration = getExpiration(confirmationDays);
        return repository.save(new ConfirmationCode(expiration, user));
    }

    @Override
    public String regenerateCode(final AppUser existingUser) {
        final Optional<ConfirmationCode> optionalCode = getConfirmationCode(existingUser);
        if (optionalCode.isPresent()) {
            final ConfirmationCode existingCode = optionalCode.get();
            final Instant newExpiration = getExpiration(confirmationDays);

            existingCode.setExpired(newExpiration);
            repository.save(existingCode);

            return existingCode.getId().toString();
        } else {
            return generateConfirmationCode(existingUser);
        }
    }

    @Override
    public ConfirmationCode getConfirmationIfValidOrThrow(final String code) {
        UUID uuid;
        try {
            uuid = UUID.fromString(code);
        } catch (IllegalArgumentException e) {
            throw new UserConfirmationException("Invalid confirmation code format");
        }

        ConfirmationCode codeEntity = repository.findById(uuid)
                .orElseThrow(() -> new UserConfirmationException("Confirmation Code not found"));

        if (codeEntity.getExpired().isBefore(Instant.now())) {
            throw new UserConfirmationException("Confirmation Code is already expired");
        }

        return codeEntity;
    }

    @Override
    public void removeToken(ConfirmationCode code) {
        repository.delete(code);
    }

    private Optional<ConfirmationCode> getConfirmationCode(AppUser user) {
        return repository.findByUser(user);
    }

    private static Instant getExpiration(int days) {
        return Instant.now().plus(Duration.ofDays(days));
    }
}
