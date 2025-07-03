package com.loudent.library.config;

import static software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import java.net.URI;
import java.util.concurrent.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

@Log4j2
@Configuration
public class DynamoDbConfig {

  @Value("${aws.dynamodb.table-prefix:dev}")
  private String tablePrefix;

  @Value("${aws.dynamodb.region:us-east-1}")
  private String awsRegion;

  @Value("${aws.dynamodb.endpoint:}")
  private String dynamoDbEndpoint;

  @Value("${aws.dynamodb.access-key:dummy}")
  private String accessKey;

  @Value("${aws.dynamodb.secret-key:dummy}")
  private String secretKey;

  private final MeterRegistry meterRegistry;

  public DynamoDbConfig(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Bean
  public DynamoDbEnhancedClient enhancedSyncClient() {
    boolean isLocal = !dynamoDbEndpoint.isBlank();
    DynamoDbClient client =
        isLocal
            ? DynamoDbClient.builder()
                .endpointOverride(URI.create(dynamoDbEndpoint))
                .region(Region.of(awsRegion))
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build()
            : DynamoDbClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

    if (isLocal) {
      log.info("Using local DynamoDB sync endpoint: {}", dynamoDbEndpoint);
    }

    return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
  }

  @Bean
  public ExecutorService dynamoAsyncExecutor() {
    int processors = Runtime.getRuntime().availableProcessors();
    int core = Math.max(8, processors);
    int max = Math.max(64, processors * 2);

    ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
            core,
            max,
            10,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadFactoryBuilder().threadNamePrefix("dynamodb-async-").build());
    executor.allowCoreThreadTimeOut(true);
    return ExecutorServiceMetrics.monitor(meterRegistry, executor, "dynamodbAsyncExecutor");
  }

  @Bean
  public DynamoDbEnhancedAsyncClient enhancedAsyncClient(ExecutorService dynamoAsyncExecutor) {
    boolean isLocal = !dynamoDbEndpoint.isBlank();

    var asyncClientBuilder =
        DynamoDbAsyncClient.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(
                isLocal
                    ? StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey))
                    : DefaultCredentialsProvider.create())
            .overrideConfiguration(
                ClientOverrideConfiguration.builder().retryPolicy(RetryMode.STANDARD).build())
            .asyncConfiguration(
                ClientAsyncConfiguration.builder()
                    .advancedOption(FUTURE_COMPLETION_EXECUTOR, dynamoAsyncExecutor)
                    .build());

    if (isLocal) {
      log.warn("Using local DynamoDB async endpoint: {}", dynamoDbEndpoint);
      asyncClientBuilder.endpointOverride(URI.create(dynamoDbEndpoint));
    }

    return DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(asyncClientBuilder.build()).build();
  }

  public String getPrefixedTableName(String baseName) {
    return tablePrefix + baseName;
  }
}
