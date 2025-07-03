package com.loudent.library.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ConcurrentUtilsTest {

  private final ExecutorService executor = Executors.newFixedThreadPool(4);

  @Test
  void parallelMap_transformsListCorrectly() {
    List<Integer> input = Arrays.asList(1, 2, 3);

    List<String> result = ConcurrentUtils.parallelMap(input, i -> "Item " + i, executor);

    assertEquals(List.of("Item 1", "Item 2", "Item 3"), result);
  }

  @Test
  void parallelMap_usesCustomExecutor() {
    AtomicInteger counter = new AtomicInteger(0);

    List<Integer> input = Arrays.asList(10, 20, 30);
    List<Integer> result =
        ConcurrentUtils.parallelMap(
            input,
            i -> {
              counter.incrementAndGet();
              return i * 2;
            },
            executor);

    assertEquals(List.of(20, 40, 60), result);
    assertEquals(3, counter.get());
  }

  @Test
  void parallelMap_throwsExceptionIfAnyTaskFails() {
    List<Integer> input = Arrays.asList(1, 2, 0); // will divide by zero

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class,
            () -> ConcurrentUtils.parallelMap(input, i -> 10 / i, executor));

    assertTrue(thrown.getCause() instanceof ArithmeticException);
  }

  @Test
  void parallelMap_handlesEmptyList() {
    List<Object> result = ConcurrentUtils.parallelMap(List.of(), x -> x, executor);

    assertTrue(result.isEmpty());
  }

  @Test
  void parallelMap_preservesOrder() {
    List<Integer> input = IntStream.range(0, 10).boxed().toList();

    List<String> result = ConcurrentUtils.parallelMap(input, i -> "v" + i, executor);

    assertEquals(IntStream.range(0, 10).mapToObj(i -> "v" + i).toList(), result);
  }
}
