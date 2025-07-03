package com.loudent.library.dao.catalog;

import static org.junit.jupiter.api.Assertions.*;

import com.loudent.library.oas.codegen.model.CatalogSearchRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class CatalogSearchExpressionBuilderTest {

  private final CatalogSearchExpressionBuilder builder = new CatalogSearchExpressionBuilder();

  @Test
  void returnsNull_whenNoFiltersPresent() {
    CatalogSearchRequest request = new CatalogSearchRequest();

    Expression result = builder.from(request);

    assertNull(result);
  }

  @Test
  void buildsExpression_forAuthorFirstNameOnly() {
    CatalogSearchRequest request = new CatalogSearchRequest().authorFirstName("Jane");

    Expression result = builder.from(request);

    assertNotNull(result);
    assertEquals("authorFirstName = :first", result.expression());

    Map<String, AttributeValue> values = result.expressionValues();
    assertEquals(1, values.size());
    assertEquals("Jane", values.get(":first").s());
  }

  @Test
  void buildsExpression_forAuthorLastNameOnly() {
    CatalogSearchRequest request = new CatalogSearchRequest().authorLastName("Smith");

    Expression result = builder.from(request);

    assertNotNull(result);
    assertEquals("authorLastName = :last", result.expression());

    Map<String, AttributeValue> values = result.expressionValues();
    assertEquals(1, values.size());
    assertEquals("Smith", values.get(":last").s());
  }

  @Test
  void buildsExpression_forBothAuthorNames() {
    CatalogSearchRequest request =
        new CatalogSearchRequest().authorFirstName("Jane").authorLastName("Smith");

    Expression result = builder.from(request);

    assertNotNull(result);
    assertTrue(result.expression().contains("authorFirstName = :first"));
    assertTrue(result.expression().contains("authorLastName = :last"));
    assertTrue(result.expression().contains(" AND "));

    Map<String, AttributeValue> values = result.expressionValues();
    assertEquals(2, values.size());
    assertEquals("Jane", values.get(":first").s());
    assertEquals("Smith", values.get(":last").s());
  }
}
