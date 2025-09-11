package com.loudent.library.util;

import groovy.util.logging.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;

@Slf4j
@Component
@Profile("inttest")
public class DynamoDbTestUtils {
  public static final String CATALOG_TABLE_NAME = "dev_Catalog";
  public static final String ACTIVITY_TABLE_NAME = "dev_Activity";
  public static final String ACCOUNT_TABLE_NAME = "dev_Accounts";

  private final DynamoDbClient dynamoDb;

  public DynamoDbTestUtils(DynamoDbClient client) {
    this.dynamoDb = client;
  }

  public void createAllTables() {
    createCatalogTable();
    createAccountTable();
    createActivityTable();
  }

  public void deleteAllTables() {
    deleteTable(CATALOG_TABLE_NAME);
    deleteTable(ACTIVITY_TABLE_NAME);
    deleteTable(ACCOUNT_TABLE_NAME);
  }

  public void createCatalogTable() {
    CreateTableRequest request =
        CreateTableRequest.builder()
            .tableName(CATALOG_TABLE_NAME)
            .keySchema(
                KeySchemaElement.builder().attributeName("isbn").keyType(KeyType.HASH).build())
            .attributeDefinitions(
                AttributeDefinition.builder()
                    .attributeName("isbn")
                    .attributeType(ScalarAttributeType.S)
                    .build())
            .provisionedThroughput(
                ProvisionedThroughput.builder()
                    .readCapacityUnits(5L)
                    .writeCapacityUnits(5L)
                    .build())
            .build();

    safeCreate(request);
  }

  public void createAccountTable() {
    CreateTableRequest request =
        CreateTableRequest.builder()
            .tableName(ACCOUNT_TABLE_NAME)
            .keySchema(
                KeySchemaElement.builder()
                    .attributeName("accountNumber")
                    .keyType(KeyType.HASH)
                    .build())
            .attributeDefinitions(
                AttributeDefinition.builder()
                    .attributeName("accountNumber")
                    .attributeType(ScalarAttributeType.S)
                    .build())
            .provisionedThroughput(
                ProvisionedThroughput.builder()
                    .readCapacityUnits(5L)
                    .writeCapacityUnits(5L)
                    .build())
            .build();

    safeCreate(request);
  }

  public void createActivityTable() {
    CreateTableRequest request =
        CreateTableRequest.builder()
            .tableName(ACTIVITY_TABLE_NAME)
            .attributeDefinitions(
                AttributeDefinition.builder()
                    .attributeName("bookId")
                    .attributeType(ScalarAttributeType.S)
                    .build(),
                AttributeDefinition.builder()
                    .attributeName("isbn")
                    .attributeType(ScalarAttributeType.S)
                    .build(),
                AttributeDefinition.builder()
                    .attributeName("accountNumber")
                    .attributeType(ScalarAttributeType.S)
                    .build())
            .keySchema(
                KeySchemaElement.builder().attributeName("bookId").keyType(KeyType.HASH).build())
            .globalSecondaryIndexes(
                GlobalSecondaryIndex.builder()
                    .indexName("isbn-index")
                    .keySchema(
                        KeySchemaElement.builder()
                            .attributeName("isbn")
                            .keyType(KeyType.HASH)
                            .build())
                    .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                    .provisionedThroughput(
                        ProvisionedThroughput.builder()
                            .readCapacityUnits(5L)
                            .writeCapacityUnits(5L)
                            .build())
                    .build(),
                GlobalSecondaryIndex.builder()
                    .indexName("account-index")
                    .keySchema(
                        KeySchemaElement.builder()
                            .attributeName("accountNumber")
                            .keyType(KeyType.HASH)
                            .build())
                    .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                    .provisionedThroughput(
                        ProvisionedThroughput.builder()
                            .readCapacityUnits(5L)
                            .writeCapacityUnits(5L)
                            .build())
                    .build())
            .provisionedThroughput(
                ProvisionedThroughput.builder()
                    .readCapacityUnits(5L)
                    .writeCapacityUnits(5L)
                    .build())
            .build();

    safeCreate(request);
  }

  private void safeCreate(CreateTableRequest request) {
    try {
      dynamoDb.createTable(request);
    } catch (ResourceInUseException e) {
      // Table already exists; ignore
    }
  }

  public void deleteTable(String tableName) {
    try {
      dynamoDb.deleteTable(DeleteTableRequest.builder().tableName(tableName).build());
    } catch (ResourceNotFoundException ignored) {
    }
  }
}
