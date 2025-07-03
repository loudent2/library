package com.loudent.library.api.error;

import com.loudent.library.oas.codegen.model.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Log4j2
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalControllerAdvice {

  @ExceptionHandler(TimeoutException.class)
  public ResponseEntity<ErrorResponse> handleTimeout(TimeoutException ex) {
    log.warn("Request timed out", ex);
    return buildError(HttpStatus.REQUEST_TIMEOUT, "The request timed out. Please try again later.");
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
    log.warn("Not found: {}", ex.getMessage());
    return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .findFirst()
            .orElse("Invalid request");

    log.warn("Validation failed: {}", message, ex);
    return buildError(HttpStatus.BAD_REQUEST, "Validation error: " + message);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArguments(IllegalArgumentException ex) {
    String message = ex.getMessage();
    log.warn("Validation failed: {}", message, ex);
    return buildError(HttpStatus.BAD_REQUEST, "Argument error: " + message);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
    String message =
        ex.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .findFirst()
            .orElse("Invalid request");

    log.warn("Constraint violation: {}", message, ex);
    return buildError(HttpStatus.BAD_REQUEST, "Validation error: " + message);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnhandled(Exception ex) {
    Throwable cause = ExceptionUtils.unwrap(ex);

    if (cause instanceof NotFoundException nf) {
      return handleNotFound(nf); // Delegate
    }

    log.error(
        "Unhandled exception",
        Map.of(
            "errorCode", "UNHANDLED_EXCEPTION",
            "exception", cause.getClass().getSimpleName(),
            "message", cause.getMessage(),
            "status", HttpStatus.INTERNAL_SERVER_ERROR.value()),
        ex);

    return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
  }

  public static ResponseEntity<ErrorResponse> buildError(HttpStatus status, String message) {
    ErrorResponse error = new ErrorResponse().code(status.value()).message(message);
    return new ResponseEntity<>(error, status);
  }
}
