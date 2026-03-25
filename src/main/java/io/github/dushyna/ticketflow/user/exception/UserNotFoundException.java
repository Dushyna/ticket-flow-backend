package io.github.dushyna.ticketflow.user.exception;

import io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends RestApiException {

    private static final String MESSAGE = "User not found";

    public UserNotFoundException() {
        super(HttpStatus.NOT_FOUND, MESSAGE);
    }
}
