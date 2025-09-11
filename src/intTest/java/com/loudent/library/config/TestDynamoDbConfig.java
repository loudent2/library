package com.loudent.library.config;

import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.GenericContainer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
@Profile("inttest")
public class TestDynamoDbConfig {
  private static final int DYNAMODB_PORT = 8000;
  private static final GenericContainer<?> DYNAMODB_CONTAINER;

  static {
    DYNAMODB_CONTAINER =
        new GenericContainer<>("amazon/dynamodb-local:latest")
            .withExposedPorts(8000)
            .withReuse(true);

    DYNAMODB_CONTAINER.setPortBindings(List.of("8000:8000"));
    DYNAMODB_CONTAINER.start();

    String endpoint =
        "http://"
            + DYNAMODB_CONTAINER.getHost()
            + ":"
            + DYNAMODB_CONTAINER.getMappedPort(DYNAMODB_PORT);
    System.setProperty("aws.dynamodb.endpoint.override", endpoint);
  }

  @Bean
  public DynamoDbClient dynamoDbClient(
      @Value("${aws.dynamodb.region}") String region,
      @Value("${aws.dynamodb.endpoint}") String endpoint) {
    return DynamoDbClient.builder()
        .region(Region.of(region))
        .endpointOverride(URI.create("http://localhost:8000"))
        .build();
  }
}
