package com.loudent.library.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class LibraryConfig {
  private static final String SERVICE = "service";
  private static final String ENVIRONMENT = "environment";

  private ExecutorService controllerExecutor;
  private ExecutorService serviceExecutor;

  @Value("${info.component:library}")
  private String componentName;

  @Value("${service.env:local}")
  private String serviceEnvironment;

  @Value("${service.concurrency.threads:125}")
  private int numberOfThreads;

  @Value("${service.requestTimeoutMs:8000}")
  private long requestTimeout;

  MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
    List<Tag> commonTags =
        List.of(Tag.of(SERVICE, componentName), Tag.of(ENVIRONMENT, serviceEnvironment));
    return registry -> registry.config().commonTags(commonTags);
  }

  @Bean(name = "controllerThreadPool")
  public ExecutorService getControllerExecutorService(MeterRegistry meterRegistry) {
    controllerExecutor =
        ExecutorServiceMetrics.monitor(
            meterRegistry, Executors.newFixedThreadPool(numberOfThreads), "controllerThreadPool");
    return controllerExecutor;
  }

  @Bean(name = "serviceThreadPool")
  public ExecutorService getServiceExecutorService(MeterRegistry meterRegistry) {
    serviceExecutor =
        ExecutorServiceMetrics.monitor(
            meterRegistry, Executors.newWorkStealingPool(), "serviceThreadPool");
    return serviceExecutor;
  }

  @PreDestroy
  public void shutdownExecutors() {
    if (controllerExecutor != null) {
      controllerExecutor.shutdown();
    }
    if (serviceExecutor != null) {
      serviceExecutor.shutdown();
    }
  }
}
