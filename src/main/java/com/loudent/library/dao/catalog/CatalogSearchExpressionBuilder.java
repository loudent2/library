package com.loudent.library.dao.catalog;

import com.loudent.library.oas.codegen.model.CatalogSearchRequest;
import java.util.*;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Component
public class CatalogSearchExpressionBuilder {

  public Expression from(CatalogSearchRequest request) {
    List<String> conditions = new ArrayList<>();
    Map<String, AttributeValue> values = new HashMap<>();

    if (request.getAuthorFirstName() != null) {
      conditions.add("authorFirstName = :first");
      values.put(":first", AttributeValue.builder().s(request.getAuthorFirstName()).build());
    }

    if (request.getAuthorLastName() != null) {
      conditions.add("authorLastName = :last");
      values.put(":last", AttributeValue.builder().s(request.getAuthorLastName()).build());
    }

    // Future fields like genre, format, etc.
    // if (request.getGenre() != null) { ... }

    if (conditions.isEmpty()) {
      return null;
    }

    return Expression.builder()
        .expression(String.join(" AND ", conditions))
        .expressionValues(values)
        .build();
  }
}
