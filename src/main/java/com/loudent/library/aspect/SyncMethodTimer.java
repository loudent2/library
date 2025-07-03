package com.loudent.library.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SyncMethodTimer extends AbstractMethodTimer {

  public SyncMethodTimer(MeterRegistry meterRegistry) {
    super(meterRegistry);
  }

  @Around("@annotation(timed)")
  public Object timeSyncMethod(ProceedingJoinPoint pjp, TimedSync timed) throws Throwable {
    String metric = timed.metric();
    Tags tags = getMetricTags(timed.tags()).and(Tags.of("method_type", "sync"));

    long startTime = System.currentTimeMillis();
    try {
      Object result = pjp.proceed();

      meterRegistry.counter(metric + SUCCESS_SUFFIX, tags).increment();
      meterRegistry
          .timer(metric + DURATION_SUFFIX, tags)
          .record(
              System.currentTimeMillis() - startTime, java.util.concurrent.TimeUnit.MILLISECONDS);

      return result;
    } catch (Throwable ex) {
      int status = mapExceptionToStatus(ex);
      Tags failureTags =
          tags.and(STATUS_TAG, String.valueOf(status), STATUS_GROUP, (status / 100) + "xx");

      meterRegistry.counter(metric + FAILURE_SUFFIX, failureTags).increment();
      meterRegistry
          .timer(metric + DURATION_SUFFIX, failureTags)
          .record(
              System.currentTimeMillis() - startTime, java.util.concurrent.TimeUnit.MILLISECONDS);

      throw ex;
    }
  }
}
