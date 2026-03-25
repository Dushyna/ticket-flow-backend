package io.github.dushyna.ticketflow.exception.handling.exceptions.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
public class RestApiException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final LocalDateTime timestamp;

    public RestApiException(HttpStatus httpStatus,
                            String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.timestamp = LocalDateTime.now();
    }
}
