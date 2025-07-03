package com.loudent.library.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

class DynamoDbConfigTest {

  private DynamoDbConfig config;

  @BeforeEach
  void setUp() {
    config = new DynamoDbConfig(new SimpleMeterRegistry());

    // Set config fields
    ReflectionTestUtils.setField(config, "tablePrefix", "test_");
    ReflectionTestUtils.setField(config, "awsRegion", "us-west-2");
    ReflectionTestUtils.setField(config, "dynamoDbEndpoint", "http://localhost:8000");
    ReflectionTestUtils.setField(config, "accessKey", "fake");
    ReflectionTestUtils.setField(config, "secretKey", "secret");
  }

  @Test
  void testGetPrefixedTableName() {
    String name = config.getPrefixedTableName("Books");
    assertThat(name).isEqualTo("test_Books");
  }

  @Test
  void testDynamoAsyncExecutorCreation() {
    ExecutorService executor = config.dynamoAsyncExecutor();
    assertThat(executor).isNotNull();
    executor.shutdownNow();
  }

  @Test
  void testEnhancedSyncClientCreation() {
    DynamoDbEnhancedClient client = config.enhancedSyncClient();
    assertThat(client).isNotNull();
  }

  @Test
  void testEnhancedAsyncClientCreation() {
    ExecutorService executor = config.dynamoAsyncExecutor();
    DynamoDbEnhancedAsyncClient client = config.enhancedAsyncClient(executor);
    assertThat(client).isNotNull();
    executor.shutdownNow();
  }

  @Test
  void testEnhancedSyncClientCreation_withDefaultAwsEndpoint() {
    // Simulate remote setup (isLocal = false)
    ReflectionTestUtils.setField(config, "dynamoDbEndpoint", "");

    DynamoDbEnhancedClient client = config.enhancedSyncClient();
    assertThat(client).isNotNull();
  }

  @Test
  void testEnhancedAsyncClientCreation_withDefaultAwsEndpoint() {
    // Simulate remote setup (isLocal = false)
    ReflectionTestUtils.setField(config, "dynamoDbEndpoint", "");

    ExecutorService executor = config.dynamoAsyncExecutor();
    DynamoDbEnhancedAsyncClient client = config.enhancedAsyncClient(executor);
    assertThat(client).isNotNull();
    executor.shutdownNow();
  }
}
