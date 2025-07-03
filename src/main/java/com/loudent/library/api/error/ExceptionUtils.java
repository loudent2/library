package com.loudent.library.api.error;

import java.util.HashSet;
import java.util.Set;

public final class ExceptionUtils {

  private ExceptionUtils() {
    // Utility class — prevent instantiation
  }

  /** Unwraps CompletionException or ExecutionException to get the underlying cause. */
  public static Throwable unwrap(Throwable t) {
    Set<Throwable> visited = new HashSet<>();
    while (t.getCause() != null && !visited.contains(t.getCause())) {
      visited.add(t);
      t = t.getCause();
    }
    return t;
  }

  /** Unwraps and casts to the given type, or returns null if it doesn't match. */
  @SuppressWarnings("unchecked")
  public static <T extends Throwable> T unwrapAndCast(Throwable t, Class<T> type) {
    Throwable unwrapped = unwrap(t);
    return type.isInstance(unwrapped) ? (T) unwrapped : null;
  }

  /** Checks if the unwrapped exception is of a certain type. */
  public static boolean isCausedBy(Throwable t, Class<? extends Throwable> type) {
    return type.isInstance(unwrap(t));
  }
}
