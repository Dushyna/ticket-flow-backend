package io.github.dushyna.ticketflow.exception.handling;

import io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException;
import io.github.dushyna.ticketflow.exception.handling.response.ErrorResponseDto;
import io.github.dushyna.ticketflow.exception.handling.response.ValidationErrorDto;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Global handler for all exceptions that threw in the application
 */
@RestControllerAdvice
@Hidden
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(RestApiException.class)
    public ResponseEntity<ErrorResponseDto> handleRestApiException(
            RestApiException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = ex.getHttpStatus();
        String rawMessage = ex.getMessage();

        if (status == HttpStatus.UNAUTHORIZED && "No cookies found!".equals(rawMessage)) {
            rawMessage = "auth.unauthorized";
        }

        String translatedMessage = translate(rawMessage);

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                translatedMessage,
                List.of(),
                request.getRequestURI()
        );

        log.info("RestApi exception caught: {}.", rawMessage);
        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, List<String>> fieldErrorsMap = new HashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String errorMessage = translate(fieldError.getDefaultMessage());

            fieldErrorsMap.computeIfAbsent(fieldError.getField(), key -> new ArrayList<>())
                    .add(errorMessage);
        }

        List<ValidationErrorDto> validationErrors = fieldErrorsMap.entrySet().stream()
                .map(entry -> new ValidationErrorDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                translate("validation.failed"),
                validationErrors,
                request.getRequestURI()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String requiredType = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "unknown";

        String message = messageSource.getMessage(
                "error.type_mismatch",
                new Object[]{ex.getName(), ex.getValue(), requiredType},
                LocaleContextHolder.getLocale()
        );

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                null,
                request.getRequestURI()
        );

        log.warn("Type mismatch exception: {}", message);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        String rootMsg = (ex.getRootCause() != null) ? ex.getRootCause().getMessage().toLowerCase() : "";
        String messageKey = "error.conflict.generic";

        if (rootMsg.contains("email")) {
            messageKey = "error.conflict.email";
        } else if (rootMsg.contains("slug")) {
            messageKey = "error.conflict.slug";
        }

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                translate(messageKey),
                null,
                request.getRequestURI()
        );

        log.warn("Data integrity violation: {}", rootMsg);
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUncaughtExceptions(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception: ", ex);
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                translate("error.internal"),
                null,
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(org.springframework.security.authorization.AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthorizationDeniedException(
            jakarta.servlet.http.HttpServletRequest request) {

        ErrorResponseDto error = new ErrorResponseDto(
                java.time.LocalDateTime.now(),
                org.springframework.http.HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                translate("auth.forbidden"),
                null,
                request.getRequestURI()
        );

        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUsernameNotFound(UsernameNotFoundException ex, HttpServletRequest request) {
        String message = translate("user.not_found");
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                message,
                null,
                request.getRequestURI()
        );
        log.warn("Login failed: {}", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());

        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                translate("auth.forbidden"),
                null,
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDto> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        String translatedMessage = translate(ex.getReason());
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                ex.getStatusCode().value(),
                HttpStatus.valueOf(ex.getStatusCode().value()).getReasonPhrase(),
                translatedMessage,
                null,
                request.getRequestURI()

        );
        return new ResponseEntity<>(error, ex.getStatusCode());
    }

    private String translate(String messageKey) {
        if (messageKey == null) return "";
        try {
            return messageSource.getMessage(messageKey, null, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            return messageKey;
        }
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpMessageNotReadableException(
            org.springframework.http.converter.HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn("Payload deserialization failed: {}", ex.getMessage());

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                translate("error.malformed_json"), // Додай цей ключ у messages
                null,
                request.getRequestURI()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleEntityNotFound(
            jakarta.persistence.EntityNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Entity not found: {}", ex.getMessage());

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(), // Or translate("error.not_found")
                null,
                request.getRequestURI()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }


}
