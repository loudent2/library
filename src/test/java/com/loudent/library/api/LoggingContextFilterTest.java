package com.loudent.library.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LoggingContextFilterTest {

  private final LoggingContextFilter filter = new LoggingContextFilter();

  @AfterEach
  void clearThreadContext() {
    ThreadContext.clearAll();
  }

  @Test
  void doFilter_shouldAddContextAndClearItAfterward() throws IOException, ServletException {
    // Arrange
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    ServletResponse mockResponse = mock(ServletResponse.class);
    FilterChain mockChain = mock(FilterChain.class);

    when(mockRequest.getMethod()).thenReturn("GET");
    when(mockRequest.getRequestURI()).thenReturn("/api/test");

    // Act
    filter.doFilter(mockRequest, mockResponse, mockChain);

    // Assert (executing this after doFilter ensures context was cleared)
    assertNull(ThreadContext.get("requestId"));
    assertNull(ThreadContext.get("method"));
    assertNull(ThreadContext.get("path"));

    verify(mockRequest).getMethod();
    verify(mockRequest).getRequestURI();
    verify(mockChain).doFilter(mockRequest, mockResponse);
  }

  @Test
  void doFilter_shouldStillClearContextIfExceptionIsThrown() throws ServletException, IOException {
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    ServletResponse mockResponse = mock(ServletResponse.class);
    FilterChain mockChain = mock(FilterChain.class);

    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getRequestURI()).thenReturn("/api/throw");

    doThrow(new ServletException("Simulated failure"))
        .when(mockChain)
        .doFilter(mockRequest, mockResponse);

    ServletException ex =
        assertThrows(
            ServletException.class,
            () -> {
              filter.doFilter(mockRequest, mockResponse, mockChain);
            });

    assertEquals("Simulated failure", ex.getMessage());

    // ThreadContext should be cleared even after exception
    assertNull(ThreadContext.get("requestId"));
    assertNull(ThreadContext.get("method"));
    assertNull(ThreadContext.get("path"));

    verify(mockRequest).getMethod();
    verify(mockRequest).getRequestURI();
    verify(mockChain).doFilter(mockRequest, mockResponse);
  }
}
