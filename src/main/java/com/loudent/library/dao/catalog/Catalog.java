package com.loudent.library.dao.catalog;

import java.util.List;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Data
public class Catalog {
  public static final String BASE_TABLE_NAME = "Catalog";

  private String isbn;
  private String title;
  private String authorFirstName;
  private String authorLastName;
  private List<String> bookIds; // Each physical copy has a bookId, e.g. "9781234567897-0"

  public static String fullTableName(String prefix) {
    return prefix + BASE_TABLE_NAME;
  }

  @DynamoDbPartitionKey
  public String getIsbn() {
    return isbn;
  }
}
