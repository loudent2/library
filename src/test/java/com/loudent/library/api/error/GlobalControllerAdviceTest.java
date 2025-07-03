package com.loudent.library.api.error;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.loudent.library.oas.codegen.model.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalControllerAdviceTest {

  private final GlobalControllerAdvice advice = new GlobalControllerAdvice();

  @Test
  void handleTimeout_returnsRequestTimeoutResponse() {
    TimeoutException ex = new TimeoutException("Simulated timeout");
    ResponseEntity<ErrorResponse> response = advice.handleTimeout(ex);

    assertEquals(HttpStatus.REQUEST_TIMEOUT, response.getStatusCode());
    assertEquals("The request timed out. Please try again later.", response.getBody().getMessage());
  }

  @Test
  void handleValidation_returnsBadRequestResponse() {
    // Mock the field error list
    var fieldErrors = TestUtils.singleFieldError("title", "must not be blank");

    // Mock the BindingResult and set up expected behavior
    var bindingResult = mock(org.springframework.validation.BindingResult.class);
    when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

    // Mock the exception and configure its getBindingResult()
    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    when(ex.getBindingResult()).thenReturn(bindingResult);

    ResponseEntity<ErrorResponse> response = advice.handleValidation(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(
        response.getBody().getMessage().contains("Validation error: title: must not be blank"));
  }

  @Test
  void handleConstraintViolation_returnsBadRequestResponse() {
    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    Path path = mock(Path.class);
    when(violation.getPropertyPath()).thenReturn(path);
    when(path.toString()).thenReturn("isbn");
    when(violation.getMessage()).thenReturn("must match pattern");

    ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

    ResponseEntity<ErrorResponse> response = advice.handleConstraintViolation(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().getMessage().contains("isbn: must match pattern"));
  }

  @Test
  void handleUnhandled_returnsInternalServerErrorResponse() {
    Exception ex = new IllegalStateException("Unexpected issue");

    ResponseEntity<ErrorResponse> response = advice.handleUnhandled(ex);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals("An unexpected error occurred.", response.getBody().getMessage());
  }

  @Test
  void handleNotFound_returnsNotFoundResponse() {
    NotFoundException ex = new NotFoundException("Catalog item not found");

    ResponseEntity<ErrorResponse> response = advice.handleUnhandled(ex);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("Catalog item not found", response.getBody().getMessage());
  }

  @Test
  void handleIllegalArguments_returnsIllegalArgumentsResponse() {
    IllegalArgumentException ex = new IllegalArgumentException("Catalog item not found");

    ResponseEntity<ErrorResponse> response = advice.handleIllegalArguments(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("Argument error: Catalog item not found", response.getBody().getMessage());
  }

  // Helper for test setup
  static class TestUtils {
    static java.util.List<org.springframework.validation.FieldError> singleFieldError(
        String field, String msg) {
      org.springframework.validation.FieldError fieldError =
          new org.springframework.validation.FieldError("object", field, msg);
      return Collections.singletonList(fieldError);
    }
  }
}
