package com.loudent.library.api.error;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

class ExceptionUtilsTest {

  @Test
  void unwrap_returnsSameIfNoWrapping() {
    Throwable t = new IllegalArgumentException("direct");
    assertSame(t, ExceptionUtils.unwrap(t));
  }

  @Test
  void unwrap_unwrapsCompletionException() {
    Throwable root = new NotFoundException("not found");
    Throwable wrapped = new CompletionException(new RuntimeException(root));

    Throwable result = ExceptionUtils.unwrap(wrapped);

    assertSame(root, result);
  }

  @Test
  void unwrap_unwrapsExecutionException() {
    Throwable root = new NotFoundException("nested");
    Throwable wrapped = new ExecutionException(new RuntimeException(root));

    Throwable result = ExceptionUtils.unwrap(wrapped);

    assertSame(root, result);
  }

  @Test
  void unwrapAndCast_returnsTargetType() {
    NotFoundException target = new NotFoundException("missing");
    Throwable wrapped = new CompletionException(new RuntimeException(target));

    NotFoundException result = ExceptionUtils.unwrapAndCast(wrapped, NotFoundException.class);

    assertSame(target, result);
  }

  @Test
  void unwrapAndCast_returnsNullIfNotFound() {
    Throwable wrapped = new CompletionException(new IllegalStateException("something else"));

    NotFoundException result = ExceptionUtils.unwrapAndCast(wrapped, NotFoundException.class);

    assertNull(result);
  }

  @Test
  void isCausedBy_returnsTrueForMatchingType() {
    Throwable wrapped =
        new CompletionException(new RuntimeException(new NotFoundException("missing")));

    assertTrue(ExceptionUtils.isCausedBy(wrapped, NotFoundException.class));
  }

  @Test
  void isCausedBy_returnsFalseForNonMatchingType() {
    Throwable wrapped =
        new CompletionException(new RuntimeException(new IllegalStateException("other")));

    assertFalse(ExceptionUtils.isCausedBy(wrapped, NotFoundException.class));
  }

  @Test
  void unwrap_handlesCyclicCause() {
    Throwable a = new RuntimeException("A");
    Throwable b = new RuntimeException("B", a);
    a.initCause(b); // create cycle: a → b → a

    Throwable result = ExceptionUtils.unwrap(a);

    assertSame(b, result); // stops before infinite loop
  }
}
