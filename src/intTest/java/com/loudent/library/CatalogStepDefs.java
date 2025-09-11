package com.loudent.library;

import static io.restassured.RestAssured.given;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.loudent.library.dao.catalog.Catalog;
import com.loudent.library.oas.codegen.model.CatalogResponse;
import com.loudent.library.util.BookData;
import com.loudent.library.util.StepHelper;
import com.loudent.library.util.TestContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest(
    classes = LibraryApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("inttest")
public class CatalogStepDefs {

  private final TestContext context;
  private final StepHelper stepHelper;

  public CatalogStepDefs(TestContext context, StepHelper stepHelper) {
    this.context = context;
    this.stepHelper = stepHelper;
  }

  @DataTableType
  public BookData bookData(Map<String, String> row) {
    return BookData.builder()
        .isbn(row.get("isbn"))
        .title(row.get("title"))
        .author(row.get("author"))
        .copies(Integer.parseInt(row.get("copies")))
        .build();
  }

  @Given("the following books exist:")
  public void theFollowingBooksExist(List<BookData> books) {
    books.forEach(
        book -> {
          Catalog item =
              stepHelper.seedCatalogItem(
                  book.getIsbn(), book.getTitle(), book.getAuthor(), book.getCopies());
          context.getExpectedBooks().add(item);
        });
  }

  @When("I search by author last name {string}")
  public void search_author_last_name(String lastName) {
    Map<String, Object> body = Map.of("authorLastName", lastName);
    context.setLastResponse(
        given().contentType("application/json").body(body).post("/catalog/search"));
  }

  @When("I request catalog by title {string}")
  public void request_by_title(String title) {
    Map<String, Object> body = Map.of("title", title);
    context.setLastResponse(
        given().contentType("application/json").body(body).post("/catalog/title"));
  }

  @When("I request catalog by isbn {string}")
  public void request_by_isbn(String isbn) {
    context.setLastResponse(StepHelper.CatalogClient.byIsbn(isbn));
  }

  @Then("the response should be for title {string}")
  public void theResponseShouldBe_forTitle(String title) {
    Catalog expectedBook =
        context.getExpectedBooks().stream()
            .filter(item -> item.getTitle().equals(title))
            .findFirst()
            .orElse(null);
    if (null == expectedBook)
      throw new RuntimeException("expected book with title " + title + " not found");
    var actual = stepHelper.read(context.getLastResponse(), CatalogResponse.class);
    assertCatalogEquals(expectedBook, actual);
    ;
  }

  @Then("the response should be for isbn {string}")
  public void theResponseShouldBe_forIsbn(String isbn) {
    Catalog expectedBook =
        context.getExpectedBooks().stream()
            .filter(item -> item.getIsbn().equals(isbn))
            .findFirst()
            .orElse(null);
    if (null == expectedBook)
      throw new RuntimeException("expected book with isbn " + isbn + " not found");
    var actual = stepHelper.read(context.getLastResponse(), CatalogResponse.class);
    assertCatalogEquals(expectedBook, actual);
  }

  @Then("Response Should include message for book title {string}")
  public void responseShouldIncludeMessageForBookTitle(String title) {
    JsonNode actualJson = stepHelper.parseJson(context.getLastResponse());
    assertThat(actualJson.path("message").asText()).isEqualTo("Book not found for title: " + title);
  }

  @Then("Response Should include message for book isbn {string}")
  public void responseShouldIncludeMessageForBookIsbn(String isbn) {
    JsonNode actualJson = stepHelper.parseJson(context.getLastResponse());
    assertThat(actualJson.path("message").asText()).isEqualTo("Book not found for ISBN: " + isbn);
  }

  @When("I search Catalog by author last name {string}")
  public void iSearchCatalogByAuthorLastName(String lastName) {
    Map<String, Object> body = Map.of("authorLastName", lastName);
    context.setLastResponse(
        given().contentType("application/json").body(body).post("/catalog/search"));
  }

  @Then("the response should include {int} books for author last name {string}")
  public void responseShouldIncludeBooksForAuthor(int expectedCount, String lastName) {
    // Build expected map (ISBN -> Catalog) and assert it's not empty right there
    Map<String, Catalog> expectedByIsbn =
        context.getExpectedBooks().stream()
            .filter(b -> lastName.equalsIgnoreCase(b.getAuthorLastName()))
            .collect(
                collectingAndThen(
                    toMap(Catalog::getIsbn, Function.identity(), (a, b) -> a, LinkedHashMap::new),
                    m -> {
                      assertThat(m).as("No expected books for author '%s'", lastName).isNotEmpty();
                      return m;
                    }));

    var actualList = stepHelper.readList(context.getLastResponse(), CatalogResponse.class);
    assertThat(actualList).hasSize(expectedCount);
    assertThat(actualList)
        .extracting(CatalogResponse::getIsbn)
        .as("response ISBNs should match expected ISBNs for author '%s'", lastName)
        .containsExactlyInAnyOrderElementsOf(expectedByIsbn.keySet());
    assertThat(actualList)
        .as("each returned book should match the catalog seed")
        .allSatisfy(
            actual -> {
              Catalog expected = expectedByIsbn.get(actual.getIsbn());
              assertThat(expected)
                  .as("Unexpected ISBN in response: %s", actual.getIsbn())
                  .isNotNull();
              assertCatalogEquals(expected, actual);
            });
  }

  @Then("the response should have no books")
  public void ResponseShouldHaveNBooks() {
    JsonNode actualJson = stepHelper.parseJson(context.getLastResponse());
    assertThat(actualJson.isArray()).isTrue();
    assertThat(actualJson).hasSize(0);
  }

  @When("I request catalog item with invalid title {string}")
  public void requestCatalogWithInvalidTitle(String title) {
    String input = "null".equals(title) ? null : title;

    Map<String, Object> body = new HashMap<>();
    body.put("title", input);
    context.setLastResponse(
        given().contentType("application/json").body(body).post("/catalog/title"));
  }

  @When("I search Catalog by bad author last name {string}")
  public void iSearchCatalogByAuthorLastName_BAD(String lastName) {
    String input = "null".equals(lastName) ? null : lastName;
    Map<String, Object> body = new HashMap<>();
    body.put("authorLastName", input);
    context.setLastResponse(
        given().contentType("application/json").body(body).post("/catalog/search"));
  }

  private void assertCatalogEquals(Catalog expectedBook, CatalogResponse actual) {
    assertThat(actual)
        .as("Catalog response should match book")
        .usingRecursiveComparison()
        .comparingOnlyFields("isbn", "title", "authorFirstName", "authorLastName")
        .isEqualTo(expectedBook);
    assertThat(actual.getTotalCopies())
        .as("Total copies should match")
        .isEqualTo(expectedBook.getBookIds().size());
  }

  @Then("catalog availability should be:")
  public void assertCatalogAvailability(DataTable table) {
    var expected =
        table.asMaps().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    r -> r.get("isbn"),
                    r -> r.get("available"),
                    (a, b) -> b,
                    java.util.LinkedHashMap::new));
    expected.forEach(
        (key, value) -> {
          var catalogResponse =
              stepHelper.read(StepHelper.CatalogClient.byIsbn(key), CatalogResponse.class);
          assertThat(catalogResponse.getAvailableCopies())
              .as("available copies should match")
              .isEqualTo(Integer.parseInt(value));
        });
  }
}
