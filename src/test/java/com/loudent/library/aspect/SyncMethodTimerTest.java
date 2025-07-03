package com.loudent.library.aspect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = SyncMethodTimerTest.Config.class)
class SyncMethodTimerTest {

  @Configuration
  @EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
  static class Config {
    @Bean
    public MeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }

    @Bean
    public SyncMethodTimer syncMethodTimer(MeterRegistry meterRegistry) {
      return new SyncMethodTimer(meterRegistry);
    }

    @Bean
    public SyncTestTarget syncTestTarget() {
      return new SyncTestTarget();
    }
  }

  @Component
  public static class SyncTestTarget {

    @TimedSync(
        metric = "test.metric",
        tags = {"env:test"})
    public String doWork() {
      return "done";
    }

    @TimedSync(
        metric = "test.fail",
        tags = {"env:test"})
    public String fail() {
      throw new RuntimeException("expected");
    }
  }

  @Autowired private SyncTestTarget target;

  @Autowired private SyncMethodTimer syncMethodTimer;

  @Autowired private MeterRegistry registry;

  @Test
  void testSuccessTimingAndCounter() {
    String result = target.doWork();
    assertEquals("done", result);

    assertEquals(1.0, registry.get("test.metric.success").counter().count(), 0.0001);
  }

  @Test
  void testFailureMetricsRecorded() {
    RuntimeException ex = assertThrows(RuntimeException.class, target::fail);
    assertEquals("expected", ex.getMessage());

    assertEquals(1.0, registry.get("test.fail.failure").counter().count(), 0.0001);
  }

  @Test
  void testRegistrySameInstance() {
    try {
      Field meterField = AbstractMethodTimer.class.getDeclaredField("meterRegistry");
      meterField.setAccessible(true);
      MeterRegistry aspectRegistry = (MeterRegistry) meterField.get(syncMethodTimer);

      assertSame(registry, aspectRegistry); // this should pass
    } catch (NoSuchFieldException | IllegalAccessException e) {
      fail("Reflection access failed: " + e.getMessage());
    }
  }
}
