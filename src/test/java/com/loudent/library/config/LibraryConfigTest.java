package com.loudent.library.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LibraryConfigTest {

  private LibraryConfig config;
  private SimpleMeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    config = new LibraryConfig();
    ReflectionTestUtils.setField(config, "componentName", "test-service");
    ReflectionTestUtils.setField(config, "serviceEnvironment", "test");
    ReflectionTestUtils.setField(config, "numberOfThreads", 10);
    ReflectionTestUtils.setField(config, "requestTimeout", 5000L);

    meterRegistry = new SimpleMeterRegistry();
  }

  @Test
  void testConfigFieldsAreSet() {
    assertThat(config.getComponentName()).isEqualTo("test-service");
    assertThat(config.getServiceEnvironment()).isEqualTo("test");
    assertThat(config.getNumberOfThreads()).isEqualTo(10);
    assertThat(config.getRequestTimeout()).isEqualTo(5000L);
  }

  @Test
  void testControllerThreadPoolIsConfigured() {
    ExecutorService executor = config.getControllerExecutorService(meterRegistry);
    assertThat(executor).isNotNull();
    executor.shutdownNow();
  }

  @Test
  void testServiceThreadPoolIsConfigured() {
    ExecutorService executor = config.getServiceExecutorService(meterRegistry);
    assertThat(executor).isNotNull();
    executor.shutdownNow();
  }

  @Test
  void testMetricsCommonTagsCustomizer() {
    var customizer = config.metricsCommonTags();
    assertThat(customizer).isNotNull();

    customizer.customize(meterRegistry);
  }

  @Test
  void shutdownExecutors_shouldShutdownAllExecutors() {
    // Manually call the methods to instantiate executors
    ExecutorService controller = config.getControllerExecutorService(meterRegistry);
    ExecutorService service = config.getServiceExecutorService(meterRegistry);

    // Shutdown
    config.shutdownExecutors();

    assertTrue(controller.isShutdown());
    assertTrue(service.isShutdown());
  }

  @Test
  void shutdownExecutors_shouldShutdownAllExecutorAreNull() {
    // Shutdown without calling any of the executors.
    config.shutdownExecutors();
    assert (true);
  }
}
