package com.loudent.library.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

public abstract class AbstractMethodTimer {
  protected static final String DURATION_SUFFIX = ".duration";
  protected static final String SUCCESS_SUFFIX = ".success";
  protected static final String FAILURE_SUFFIX = ".failure";
  protected static final String STATUS_TAG = "http_status";
  protected static final String STATUS_GROUP = "status_group";
  protected static final String SEPARATOR = ":";

  protected final MeterRegistry meterRegistry;

  protected AbstractMethodTimer(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  protected Method getMethod(ProceedingJoinPoint invocation) {
    MethodSignature signature = (MethodSignature) invocation.getSignature();
    return signature.getMethod();
  }

  protected Tags getMetricTags(String[] tagsInAnnotation) {
    if (tagsInAnnotation == null || tagsInAnnotation.length == 0) {
      return Tags.empty();
    }
    List<Tag> tagList =
        Arrays.stream(tagsInAnnotation)
            .map(AbstractMethodTimer::convertTag)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    return Tags.of(tagList);
  }

  private static Optional<Tag> convertTag(String tagValue) {
    String[] pair = tagValue.split(SEPARATOR);
    return pair.length == 2 ? Optional.of(Tag.of(pair[0], pair[1])) : Optional.empty();
  }

  protected int mapExceptionToStatus(Throwable ex) {
    return 500; // Future-proof if you want to refine later
  }
}
