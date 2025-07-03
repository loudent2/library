package com.loudent.library.aspect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = AsyncMethodTimerTest.Config.class)
class AsyncMethodTimerTest {

  @Configuration
  @EnableAspectJAutoProxy(exposeProxy = true)
  static class Config {

    @Bean
    public MeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }

    @Bean
    public AsyncMethodTimer asyncMethodTimer(MeterRegistry meterRegistry) {
      return new AsyncMethodTimer(meterRegistry);
    }

    @Bean
    public AsyncTestTarget asyncTestTarget() {
      return new AsyncTestTarget();
    }
  }

  static class AsyncTestTarget {

    @TimedAsync(
        metric = "async.test",
        tags = {"env:test"})
    public CompletableFuture<ResponseEntity<String>> succeed() {
      return CompletableFuture.completedFuture(ResponseEntity.ok("ok"));
    }

    @TimedAsync(
        metric = "async.fail",
        tags = {"env:test"})
    public CompletableFuture<ResponseEntity<String>> fail() {
      CompletableFuture<ResponseEntity<String>> f = new CompletableFuture<>();
      f.completeExceptionally(new RuntimeException("failure"));
      return f;
    }

    @TimedAsync(metric = "")
    public CompletableFuture<ResponseEntity<String>> noArgsFail() {
      CompletableFuture<ResponseEntity<String>> f = new CompletableFuture<>();
      f.completeExceptionally(new RuntimeException("failure"));
      return f;
    }
  }

  @Autowired AsyncTestTarget asyncTestTarget;

  @Autowired MeterRegistry meterRegistry;

  private AsyncMethodTimer asyncTimer;

  @BeforeEach
  void setUp() {
    asyncTimer = new AsyncMethodTimer(meterRegistry);
  }

  @Test
  void testSuccessMetrics() throws Exception {
    asyncTestTarget.succeed().join();

    assertEquals(1.0, meterRegistry.get("async.test.success").counter().count(), 0.01);
  }

  @Test
  void testFailureMetrics() {
    assertThrows(RuntimeException.class, () -> asyncTestTarget.fail().join());

    assertEquals(1.0, meterRegistry.get("async.fail.failure").counter().count(), 0.01);
  }

  @Test
  void testFailureMetricsNoArgs() {
    assertThrows(RuntimeException.class, () -> asyncTestTarget.noArgsFail().join());

    assertEquals(1.0, meterRegistry.get("noArgsFail.failure").counter().count(), 0.01);
  }

  @Test
  void timeAroundAsyncMethod_shouldRecordExceptionWhenThrowableThrown() throws Throwable {
    ProceedingJoinPoint mockJoinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature mockSignature = mock(MethodSignature.class);
    Method dummyMethod = String.class.getMethod("toString");

    when(mockSignature.getMethod()).thenReturn(dummyMethod);
    when(mockJoinPoint.getSignature()).thenReturn(mockSignature);
    when(mockJoinPoint.getTarget()).thenReturn(new Object());
    when(mockJoinPoint.proceed()).thenThrow(new RuntimeException("Simulated failure"));

    TimedAsync mockAnnotation = mock(TimedAsync.class);
    when(mockAnnotation.metric()).thenReturn("test.metric"); // 👈 Needed
    when(mockAnnotation.tags()).thenReturn(new String[] {"component:test"}); // 👈 Optional but safe

    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> asyncTimer.timeAroundAsyncMethod(mockJoinPoint, mockAnnotation));

    assertEquals("Simulated failure", ex.getMessage());
  }
}
