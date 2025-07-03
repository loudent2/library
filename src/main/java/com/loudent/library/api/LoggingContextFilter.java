package com.loudent.library.api;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class LoggingContextFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String requestId = UUID.randomUUID().toString();

    // Add contextual metadata
    ThreadContext.put("requestId", requestId);
    ThreadContext.put("method", httpRequest.getMethod());
    ThreadContext.put("path", httpRequest.getRequestURI());

    try {
      chain.doFilter(request, response);
    } finally {
      ThreadContext.clearAll(); // Prevent context leakage across threads
    }
  }
}
