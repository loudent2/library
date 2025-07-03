package com.loudent.library.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Log4j2
public class AsyncMethodTimer extends AbstractMethodTimer {

  public AsyncMethodTimer(MeterRegistry meterRegistry) {
    super(meterRegistry);
  }

  @Around("@annotation(timedAsync)")
  public Object timeAroundAsyncMethod(ProceedingJoinPoint joinPoint, TimedAsync timedAsync)
      throws Throwable {
    Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
    String metricName = timedAsync.metric().isEmpty() ? method.getName() : timedAsync.metric();
    Tags dynamicTags = getMetricTags(timedAsync.tags()).and(Tags.of("method_type", "async"));

    var timer = startTimer();

    try {
      CompletableFuture<?> future = (CompletableFuture<?>) joinPoint.proceed();
      future.whenComplete(
          (result, error) -> {
            String status = "200";
            String statusGroup = "2xx";

            if (result instanceof ResponseEntity<?> response) {
              int code = response.getStatusCode().value();
              status = String.valueOf(code);
              statusGroup = code / 100 + "xx";
            }

            if (error != null) {
              log.warn("Async error in {}: {}", metricName, error.toString());
              status = "500";
              statusGroup = "5xx";
            }

            recordTimer(
                metricName, timer, dynamicTags.and("status", status, "status_group", statusGroup));
          });
      return future;
    } catch (Throwable t) {
      recordTimer(metricName, timer, dynamicTags.and("status", "500", "status_group", "5xx"));
      throw t;
    }
  }

  private TimerContext startTimer() {
    return new TimerContext(System.nanoTime());
  }

  private void recordTimer(String name, TimerContext context, Tags tags) {
    long duration = System.nanoTime() - context.startTime;
    meterRegistry
        .timer(name + ".duration", tags)
        .record(duration, java.util.concurrent.TimeUnit.NANOSECONDS);

    String status =
        tags.stream()
            .filter(tag -> "status".equals(tag.getKey()))
            .map(Tag::getValue)
            .findFirst()
            .orElse("500");

    if ("200".equals(status)) {
      meterRegistry.counter(name + ".success", tags).increment();
    } else {
      meterRegistry.counter(name + ".failure", tags).increment();
    }
  }

  private static class TimerContext {
    private final long startTime;

    TimerContext(long startTime) {
      this.startTime = startTime;
    }
  }
}
