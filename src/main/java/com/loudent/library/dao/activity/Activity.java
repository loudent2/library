package com.loudent.library.dao.activity;

import java.time.LocalDate;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
@Data
public class Activity {
  public static final String BASE_TABLE_NAME = "Activity";
  public static final String ISBN_INDEX = "isbn-index";
  public static final String ACCOUNT_INDEX = "account-index";

  private String bookId; // e.g. 9781234567897-2
  private String isbn;
  private String title;
  private String accountNumber;
  private LocalDate checkOutDate;
  private LocalDate dueDate;

  public static String fullTableName(String prefix) {
    return prefix + BASE_TABLE_NAME;
  }

  @DynamoDbPartitionKey
  public String getBookId() {
    return bookId;
  }

  @DynamoDbSecondaryPartitionKey(indexNames = ISBN_INDEX)
  public String getIsbn() {
    return isbn;
  }

  @DynamoDbSecondaryPartitionKey(indexNames = ACCOUNT_INDEX)
  public String getAccountNumber() {
    return accountNumber;
  }
}
