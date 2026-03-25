package io.github.dushyna.ticketflow.user.exception;

import io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException;
import org.springframework.http.HttpStatus;

public class UserAlreadyExistException extends RestApiException {

    public UserAlreadyExistException() {
        super(HttpStatus.CONFLICT, "User already exists");
    }
    public UserAlreadyExistException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
