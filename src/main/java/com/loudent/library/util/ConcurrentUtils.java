package com.loudent.library.util;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class ConcurrentUtils {

  private ConcurrentUtils() {
    // utility class
  }

  public static <T, R> List<R> parallelMap(
      Collection<T> input, Function<T, R> mapper, Executor executor) {
    List<CompletableFuture<R>> futures =
        input.stream()
            .map(item -> CompletableFuture.supplyAsync(() -> mapper.apply(item), executor))
            .toList();

    // Block until all are done
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    // Collect results
    return futures.stream().map(CompletableFuture::join).toList();
  }
}
