package io.github.dushyna.ticketflow.mail.confirmation.code.interfaces;

import io.github.dushyna.ticketflow.mail.confirmation.code.ConfirmationCode;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.exception.UserConfirmationException;

public interface ConfirmationService {

    String generateConfirmationCode(AppUser user);

    ConfirmationCode generateConfirmation(AppUser user);

    String regenerateCode(AppUser existingUser);

    ConfirmationCode getConfirmationIfValidOrThrow(String code) throws UserConfirmationException;

    void removeToken(ConfirmationCode code);
}
