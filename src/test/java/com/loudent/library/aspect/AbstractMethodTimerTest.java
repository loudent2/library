package com.loudent.library.aspect;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;

class AbstractMethodTimerTest {

  static class TestMethodTimer extends AbstractMethodTimer {
    TestMethodTimer() {
      super(new SimpleMeterRegistry());
    }

    // Expose protected method for testing
    public Tags testGetMetricTags(String[] tags) {
      return getMetricTags(tags);
    }

    public int testMapExceptionToStatus(Throwable ex) {
      return mapExceptionToStatus(ex);
    }
  }

  final TestMethodTimer timer = new TestMethodTimer();

  @Test
  void testGetMetricTags_validTags() {
    Tags tags = timer.testGetMetricTags(new String[] {"foo:bar", "env:test"});
    List<Tag> tagList = tags.stream().toList();

    assertEquals(2, tagList.size());
    assertEquals("foo", tagList.get(1).getKey());
    assertEquals("bar", tagList.get(1).getValue());
    assertEquals("env", tagList.get(0).getKey());
    assertEquals("test", tagList.get(0).getValue());
  }

  @Test
  void testGetMetricTags_invalidTags() {
    Tags tags = timer.testGetMetricTags(new String[] {"foobar", "x:y:z"});
    assertTrue(tags.stream().toList().isEmpty(), "Invalid tags should be ignored");
  }

  @Test
  void testGetMetricTags_nullOrEmpty() {
    assertEquals(Tags.empty(), timer.testGetMetricTags(null));
    assertEquals(Tags.empty(), timer.testGetMetricTags(new String[] {}));
  }

  @Test
  void testMapExceptionToStatus_defaultAlways500() {
    assertEquals(500, timer.testMapExceptionToStatus(new RuntimeException()));
  }

  @Test
  void getMethod_shouldReturnMethodFromJoinPoint() throws NoSuchMethodException {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    Method expectedMethod = String.class.getMethod("toString");
    MethodSignature signature = mock(MethodSignature.class);
    when(signature.getMethod()).thenReturn(expectedMethod);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method actualMethod = timer.getMethod(joinPoint);

    assertEquals(expectedMethod, actualMethod);
  }
}
