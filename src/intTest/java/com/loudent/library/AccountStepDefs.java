package com.loudent.library;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.loudent.library.dao.account.Account;
import com.loudent.library.oas.codegen.model.BorrowedBook;
import com.loudent.library.oas.codegen.model.UserResponse;
import com.loudent.library.util.AccountData;
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
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest(
    classes = LibraryApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("inttest")
public class AccountStepDefs {
  private final TestContext context;
  private final StepHelper stepHelper;

  public AccountStepDefs(TestContext context, StepHelper stepHelper) {
    this.context = context;
    this.stepHelper = stepHelper;
  }

  @DataTableType
  public AccountData accountData(Map<String, String> row) {
    return AccountData.builder()
        .accountNumber(row.get("accountNumber"))
        .name(row.get("name"))
        .memberSince(LocalDate.parse(row.get("memberSince")))
        .build();
  }

  @Given("the following accounts exist:")
  public void theFollowingUsersExist(List<AccountData> users) {
    users.forEach(
        account -> {
          Account item =
              stepHelper.seedAccountItem(
                  account.getAccountNumber(), account.getName(), account.getMemberSince());
          context.getExpectedAccounts().add(item);
        });
  }

  @When("I request account {string}")
  public void iRequestAccount(String accountNumber) {
    context.setLastResponse(StepHelper.AccountClient.byAccount(accountNumber));
  }

  @Then("the response should be for account {string}")
  public void theResponseShouldBeTheAccount(String accountNumber) {
    Account expectedAccount =
        context.getExpectedAccounts().stream()
            .filter(item -> item.getAccountNumber().equals(accountNumber))
            .findFirst()
            .orElse(null);
    if (null == expectedAccount)
      throw new RuntimeException("expected user with account" + accountNumber + " not found");

    UserResponse actual = stepHelper.read(context.getLastResponse(), UserResponse.class);
    assertThat(actual)
        .as("user view should match account")
        .usingRecursiveComparison()
        .comparingOnlyFields("accountNumber", "firstName", "lastName", "memberSince")
        .isEqualTo(expectedAccount);
  }

  @When("I request account with invalid number {string}")
  public void requestAccountWithInvalidNumber(String accountNumber) {
    String input = "null".equals(accountNumber) ? null : accountNumber;
    context.setLastResponse(StepHelper.AccountClient.byAccount(input));
  }

  @Then("user {string} should have no borrowed books:")
  public void assertUserBorrowedNone(String account) {
    UserResponse user =
        stepHelper.read(given().get("/user/{accountNumber}", account), UserResponse.class);
    assertThat(user.getBorrowedBooks())
        .as("should have no borrowed booksfor %s", account)
        .isEmpty();
  }

  @Then("user {string} should have borrowed books:")
  public void assertUserBorrowed(String account, DataTable table) {
    var expectedBookIds = table.asMaps().stream().flatMap(map -> map.values().stream()).toList();
    UserResponse user =
        stepHelper.read(StepHelper.AccountClient.byAccount(account), UserResponse.class);
    List<String> actualIds =
        Optional.ofNullable(user.getBorrowedBooks()).orElse(List.of()).stream()
            .map(BorrowedBook::getBookId)
            .toList();

    assertThat(actualIds)
        .as("borrowed bookIds for user %s", account)
        .containsExactlyInAnyOrderElementsOf(expectedBookIds);
  }
}
