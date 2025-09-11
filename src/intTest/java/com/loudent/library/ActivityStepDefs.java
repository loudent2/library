package com.loudent.library;

import com.loudent.library.dao.activity.Activity;
import com.loudent.library.oas.codegen.model.BookOperationResult;
import com.loudent.library.util.ActivityData;
import com.loudent.library.util.StepHelper;
import com.loudent.library.util.TestContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest(
    classes = LibraryApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("inttest")
public class ActivityStepDefs {
  private final TestContext context;
  private final StepHelper stepHelper;

  public ActivityStepDefs(TestContext context, StepHelper stepHelper) {
    this.context = context;
    this.stepHelper = stepHelper;
  }

  @DataTableType
  public ActivityData activityData(Map<String, String> row) {
    return ActivityData.builder()
        .bookId(row.get("bookId"))
        .isbn(row.get("isbn"))
        .accountNumber(row.get("accountNumber"))
        .title(row.get("title"))
        .checkOutDate(LocalDate.parse(row.get("checkOutDate")))
        .dueDate(LocalDate.parse(row.get("dueDate")))
        .build();
  }

  @Given("the following book ids exist")
  public void existingBooks(List<ActivityData> activities) {
    activities.forEach(
        activity -> {
          Activity item =
              stepHelper.seedActivityItem(
                  activity.getBookId(),
                  activity.getIsbn(),
                  activity.getAccountNumber(),
                  activity.getTitle(),
                  activity.getDueDate(),
                  activity.getCheckOutDate());
          context.getExpectedActivities().add(item);
        });
  }

  @When("I checkout books for account {string}:")
  public void checkoutBooks(String account, DataTable table) {
    var bookIds = table.asMaps().stream().flatMap(map -> map.values().stream()).toList();
    var response = StepHelper.ActivityClient.checkout(account, bookIds);
    stepHelper.assert200(response);
    context.setLastResponse(response);
  }

  @Then("the activity operation notes should be:")
  public void assertCheckoutNotes(DataTable table) {
    var expected =
        table.asMaps().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    r -> r.get("bookId"),
                    r -> r.get("note"),
                    (a, b) -> b,
                    java.util.LinkedHashMap::new));

    var results = stepHelper.readList(context.getLastResponse(), BookOperationResult.class);
    Map<String, String> actual =
        results.stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    BookOperationResult::getBookId,
                    r -> r.getNotes() == null ? "ERROR" : r.getNotes().trim(),
                    (a, b) -> b, // if dup bookIds, keep last
                    java.util.LinkedHashMap::new));
    org.assertj.core.api.Assertions.assertThat(actual)
        .as("bookId -> note map should match")
        .containsExactlyInAnyOrderEntriesOf(expected);
  }

  @When("I checkin books:")
  public void i_check_in_books(DataTable table) {
    var bookIds = table.asMaps().stream().flatMap(map -> map.values().stream()).toList();
    var response = StepHelper.ActivityClient.checkin(bookIds);
    stepHelper.assert200(response);
    context.setLastResponse(response);
  }
}
