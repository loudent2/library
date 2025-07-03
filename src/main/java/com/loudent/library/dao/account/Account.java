package com.loudent.library.dao.account;

import java.time.LocalDate;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Data
public class Account {
  public static final String BASE_TABLE_NAME = "Account";
  private String accountNumber;
  private String firstName;
  private String lastName;
  private LocalDate memberSince;

  public static String fullTableName(String prefix) {
    return prefix + BASE_TABLE_NAME;
  }

  @DynamoDbPartitionKey
  public String getAccountNumber() {
    return accountNumber;
  }
}
