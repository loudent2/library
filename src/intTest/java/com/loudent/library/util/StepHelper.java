package com.loudent.library.util;

import static com.loudent.library.util.DynamoDbTestUtils.ACCOUNT_TABLE_NAME;
import static com.loudent.library.util.DynamoDbTestUtils.ACTIVITY_TABLE_NAME;
import static com.loudent.library.util.DynamoDbTestUtils.CATALOG_TABLE_NAME;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loudent.library.dao.account.Account;
import com.loudent.library.dao.activity.Activity;
import com.loudent.library.dao.catalog.Catalog;
import com.loudent.library.oas.codegen.model.CheckoutRequest;
import io.cucumber.datatable.DataTable;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Component
@Slf4j
public class StepHelper {

  private final ObjectMapper objectMapper;
  private final DynamoDbEnhancedClient enhancedClient;

  public StepHelper(ObjectMapper objectMapper, DynamoDbEnhancedClient enhancedClient) {
    this.objectMapper = objectMapper;
    this.enhancedClient = enhancedClient;
  }

  /* ==================== JSON ==================== */

  public JsonNode parseJson(Response response) {
    try {
      var jsonReturn = objectMapper.readTree(response.getBody().asString());
      log.info("parsed Response: {}", jsonReturn);
      return jsonReturn;
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse JSON", e);
    }
  }

  public void assert200(Response r) {
    assertThat(r.statusCode())
        .as("Expected 200 but got %s with body: %s", r.statusCode(), r.getBody().asString())
        .isEqualTo(200);
  }

  public <T> T read(Response r, Class<T> type) {
    try {
      return objectMapper.readValue(r.getBody().asByteArray(), type);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse JSON to " + type.getSimpleName(), e);
    }
  }

  public <T> T read(Response r, TypeReference<T> typeRef) {
    try {
      return objectMapper.readValue(r.getBody().asByteArray(), typeRef);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse JSON to TypeReference " + typeRef.getType(), e);
    }
  }

  public <T> List<T> readList(Response r, Class<T> elementType) {
    try {
      byte[] bytes = r.getBody().asByteArray();
      JavaType listType =
          objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
      return objectMapper.readValue(bytes, listType);
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to parse JSON to List<" + elementType.getSimpleName() + ">", e);
    }
  }

  /* ==================== SEED ==================== */

  public Catalog seedCatalogItem(String isbn, String title, String author, int copies) {
    Catalog item = new Catalog();
    item.setIsbn(isbn);
    item.setTitle(title);

    List<String> copyList =
        IntStream.range(0, copies).mapToObj(i -> isbn + "." + i).collect(Collectors.toList());
    item.setBookIds(copyList);
    String[] parts = author.trim().split("\\s+", 2);
    String firstName = parts.length > 0 ? parts[0] : "";
    String lastName = parts.length > 1 ? parts[1] : "";

    item.setAuthorFirstName(firstName);
    item.setAuthorLastName(lastName);

    enhancedClient.table(CATALOG_TABLE_NAME, TableSchema.fromBean(Catalog.class)).putItem(item);
    return item;
  }

  public Account seedAccountItem(String accountNumber, String name, LocalDate memberSince) {

    Account user = new Account();
    user.setAccountNumber(accountNumber);
    String[] parts = name.trim().split("\\s+", 2);
    String firstName = parts.length > 0 ? parts[0] : "";
    String lastName = parts.length > 1 ? parts[1] : "";

    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setMemberSince(memberSince);

    enhancedClient.table(ACCOUNT_TABLE_NAME, TableSchema.fromBean(Account.class)).putItem(user);
    return user;
  }

  public Activity seedActivityItem(
      String bookId,
      String isbn,
      String accountNumber,
      String title,
      LocalDate dueDate,
      LocalDate checkoutDate) {
    Activity activity = new Activity();
    activity.setBookId(bookId);
    activity.setIsbn(isbn);
    activity.setAccountNumber(accountNumber);
    activity.setTitle(title);
    activity.setDueDate(dueDate);
    activity.setCheckOutDate(checkoutDate);
    enhancedClient
        .table(ACTIVITY_TABLE_NAME, TableSchema.fromBean(Activity.class))
        .putItem(activity);

    return activity;
  }

  /* ==================== DataTable  ==================== */

  public List<Map<String, String>> rows(DataTable t) {
    return t.asMaps(String.class, String.class);
  }

  public List<String> column(DataTable t, String col) {
    return rows(t).stream()
        .map(m -> m.get(col))
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }

  public Map<String, String> pair(DataTable t, String keyCol, String valCol) {
    return rows(t).stream()
        .collect(
            java.util.stream.Collectors.toMap(
                m -> m.get(keyCol), m -> m.get(valCol), (a, b) -> b, java.util.LinkedHashMap::new));
  }

  /* ------------ Static, stateless clients (return Response) ------------ */

  public static final class ActivityClient {
    private ActivityClient() {}

    public static Response checkout(String account, List<String> bookIds) {
      var body = new CheckoutRequest(account, bookIds);
      return given().contentType("application/json").body(body).post("/activity/checkout");
    }

    public static Response checkin(List<String> bookIds) {
      Map<String, Object> body = Map.of("bookIds", bookIds);
      return given().contentType("application/json").body(body).post("/activity/checkin");
    }
  }

  public static final class CatalogClient {
    private CatalogClient() {}

    public static Response byIsbn(String isbn) {
      return given().get("/catalog/isbn/{isbn}", isbn);
    }
  }

  public static final class AccountClient {
    private AccountClient() {}

    public static Response byAccount(String account) {
      return given().get("/user/{accountNumber}", account);
    }
  }
}
