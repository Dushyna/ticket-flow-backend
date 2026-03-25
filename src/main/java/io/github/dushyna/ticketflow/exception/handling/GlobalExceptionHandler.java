package io.github.dushyna.ticketflow.exception.handling;

import io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException;
import io.github.dushyna.ticketflow.exception.handling.response.ErrorResponseDto;
import io.github.dushyna.ticketflow.exception.handling.response.ValidationErrorDto;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global handler for all exceptions that threw in the application
 */
@RestControllerAdvice
@Hidden
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RestApiException.class)
    public ResponseEntity<ErrorResponseDto> handleRestApiException(
            RestApiException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = ex.getHttpStatus();
        String message = ex.getMessage();
        if (status == HttpStatus.UNAUTHORIZED && "No cookies found!".equals(message)) {
            message = "Authorization required: token missing";
        }

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                List.of(),
                request.getRequestURI()
        );
        log.info("RestApi exception caught: {}.", ExceptionUtils.getMessage(ex), ex);
        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, List<String>> fieldErrorsMap = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrorsMap.computeIfAbsent(fieldError.getField(), key -> new ArrayList<>())
                    .add(fieldError.getDefaultMessage());
        }

        List<ValidationErrorDto> validationErrors = fieldErrorsMap.entrySet().stream()
                .map(entry -> new ValidationErrorDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed for one or more fields",
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

        String message = String.format("The parameter '%s' has an invalid value: '%s'. Expected type: %s",
                ex.getName(), ex.getValue(), requiredType);

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                List.of(),
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
        String message = "Database conflict: a record with this value already exists (e.g., email or organization slug).";

        if (ex.getRootCause() != null) {
            String rootMsg = ex.getRootCause().getMessage();
            if (rootMsg != null && rootMsg.contains("detail")) {
                message = rootMsg.substring(rootMsg.indexOf("detail") + 7).replace(")", "").replace("(", "");
            }
        }

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                message,
                List.of(),
                request.getRequestURI()
        );

        log.warn("Data integrity violation: {}", message);
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUncaughtExceptions(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception: ", ex);
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected error occurred. Please contact support.",
                List.of(),
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
                "Forbidden",
                "Access Denied: You do not have the required permissions.",
                java.util.Collections.emptyList(),
                request.getRequestURI()
        );

        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUsernameNotFound(UsernameNotFoundException ex, HttpServletRequest request) {
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage(),
                null,
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }


//    *
//     * Delegate any AuthenticationException (401 Unauthorized)
//     * to the RestAuthenticationEntryPoint, so it renders your JSON.
//
//    @ExceptionHandler(AuthenticationException.class)
//    public void handleAuthenticationException(
//            AuthenticationException ex,
//            HttpServletRequest request,
//            HttpServletResponse response
//    ) throws IOException {
//        log.warn("Authentication failure: {}", ex.getMessage());
//        new RestAuthenticationEntryPoint().commence(request, response, ex);
//    }
//
//    *
//     * Delegate any AccessDeniedException (403 Forbidden)
//     * to the CustomAccessDeniedHandler.
//
//    @ExceptionHandler(AccessDeniedException.class)
//    public void handleAccessDeniedException(
//            AccessDeniedException ex,
//            HttpServletRequest request,
//            HttpServletResponse response
//    ) throws IOException {
//        log.warn("Access denied: {}", ex.getMessage());
//        new CustomAccessDeniedHandler().handle(request, response, ex);
//    }
//
//    *
//     * Fallback for any other exceptions.
//     * Logs the error and returns 500 with a generic message.

}
