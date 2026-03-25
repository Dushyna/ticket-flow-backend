package io.github.dushyna.ticketflow.user.exception;

import io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException;
import org.springframework.http.HttpStatus;

public class UserConfirmationException extends RestApiException {

    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public UserConfirmationException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
