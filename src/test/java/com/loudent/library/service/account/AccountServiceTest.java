package com.loudent.library.service.account;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.loudent.library.api.error.NotFoundException;
import com.loudent.library.config.DynamoDbConfig;
import com.loudent.library.dao.account.Account;
import com.loudent.library.dao.activity.Activity;
import com.loudent.library.oas.codegen.model.BorrowedBook;
import com.loudent.library.oas.codegen.model.UserResponse;
import com.loudent.library.service.activity.ActivityService;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  @Mock private DynamoDbEnhancedAsyncClient client;
  @Mock private DynamoDbAsyncTable<Account> accountTable;
  @Mock private DynamoDbConfig config;
  @Mock private ActivityService activityService;

  @InjectMocks private AccountService accountService;

  private Account sampleAccount;
  private Activity sampleActivity;

  @BeforeEach
  void setup() {
    // Set up mocks
    when(config.getPrefixedTableName("Accounts")).thenReturn("Accounts");
    when(client.table(eq("Accounts"), any(TableSchema.class))).thenReturn(accountTable);

    // Manually call constructor to trigger real initialization
    accountService = new AccountService(client, config, activityService);

    // Sample account
    sampleAccount = new Account();
    sampleAccount.setAccountNumber("ACC123");
    sampleAccount.setFirstName("Jane");
    sampleAccount.setLastName("Doe");
    sampleAccount.setMemberSince(LocalDate.of(2020, 1, 1));

    // Sample activity
    sampleActivity = new Activity();
    sampleActivity.setAccountNumber("ACC123");
    sampleActivity.setBookId("9781234567890.001");
    sampleActivity.setIsbn("9781234567890");
    sampleActivity.setTitle("Some Book");
    sampleActivity.setCheckOutDate(LocalDate.of(2024, 6, 1));
    sampleActivity.setDueDate(LocalDate.of(2024, 7, 1));
  }

  @Test
  void getByAccountNumber_success_withBorrowedBooks() {
    when(accountTable.getItem(any(GetItemEnhancedRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(sampleAccount));
    when(activityService.getByAccountNumber("ACC123")).thenReturn(List.of(sampleActivity));

    UserResponse result = accountService.getByAccountNumber("ACC123");

    assertNotNull(result);
    assertEquals("ACC123", result.getAccountNumber());
    assertEquals("Jane", result.getFirstName());
    assertEquals(1, result.getBorrowedBooks().size());

    BorrowedBook borrowed = result.getBorrowedBooks().get(0);
    assertEquals("9781234567890.001", borrowed.getBookId());
    assertEquals("Some Book", borrowed.getTitle());
    assertEquals(LocalDate.of(2024, 6, 1), borrowed.getCheckOutDate());
    assertEquals(LocalDate.of(2024, 7, 1), borrowed.getDueByDate());
  }

  @Test
  void getByAccountNumber_success_noBorrowedBooks() {
    when(accountTable.getItem(any(GetItemEnhancedRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(sampleAccount));
    when(activityService.getByAccountNumber("ACC123")).thenReturn(Collections.emptyList());

    UserResponse result = accountService.getByAccountNumber("ACC123");

    assertNotNull(result);
    assertEquals("ACC123", result.getAccountNumber());
    assertNotNull(result.getBorrowedBooks());
    assertTrue(result.getBorrowedBooks().isEmpty());
  }

  @Test
  void getByAccountNumber_accountNotFound() {
    when(accountTable.getItem(any(GetItemEnhancedRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(null));

    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> {
              accountService.getByAccountNumber("MISSING");
            });

    assertTrue(ex.getMessage().contains("Failed to retrieve account #MISSING"));
    assertInstanceOf(NotFoundException.class, ex.getCause());
  }

  @Test
  void mapToUserResponse_returnsNullForNullInput() {
    assertNull(accountService.mapToUserResponse(null));
  }

  @Test
  void mapToUserResponse_mapsCorrectly() {
    UserResponse response = accountService.mapToUserResponse(sampleAccount);
    assertEquals("ACC123", response.getAccountNumber());
    assertEquals("Jane", response.getFirstName());
    assertEquals("Doe", response.getLastName());
    assertEquals(LocalDate.of(2020, 1, 1), response.getMemberSince());
  }

  @Test
  void enrichWithBorrowedBooks_nullSafeActivityList() {
    when(activityService.getByAccountNumber("ACC123")).thenReturn(null);

    UserResponse base = new UserResponse().accountNumber("ACC123");
    UserResponse enriched = accountService.enrichWithBorrowedBooks(base, "ACC123");

    assertNotNull(enriched.getBorrowedBooks());
    assertTrue(enriched.getBorrowedBooks().isEmpty());
  }

  @Test
  void mapToBorrowedBook_mapsAllFields() {
    BorrowedBook book = accountService.mapToBorrowedBook(sampleActivity);
    assertEquals("9781234567890.001", book.getBookId());
    assertEquals("Some Book", book.getTitle());
    assertEquals(LocalDate.of(2024, 6, 1), book.getCheckOutDate());
    assertEquals(LocalDate.of(2024, 7, 1), book.getDueByDate());
  }

  @Test
  void accountExists_shouldReturnTrueIfAccountExists() {
    when(accountTable.getItem(any(GetItemEnhancedRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(sampleAccount));

    assertTrue(accountService.accountExists("ACC123"));
  }

  @Test
  void accountExists_shouldReturnFalseIfAccountMissing() {
    when(accountTable.getItem(any(Consumer.class)))
        .thenReturn(CompletableFuture.completedFuture(null));

    assertFalse(accountService.accountExists("ACC404"));
  }

  @Test
  void accountExists_shouldReturnFalseOnException() {
    when(accountTable.getItem(any(Consumer.class)))
        .thenThrow(new RuntimeException("Simulated error"));

    assertFalse(accountService.accountExists("ACCERR"));
  }

  @Test
  void requestForAccount_shouldBuildCorrectKey() {
    GetItemEnhancedRequest request = accountService.requestForAccount("ACC123");

    Key key = request.key();
    assertNotNull(key);
    assertEquals("ACC123", key.partitionKeyValue().s());
  }

  @Test
  void accountExists_shouldReturnFalseIfAccountIsNull() {
    when(accountTable.getItem(any(GetItemEnhancedRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(null));

    assertFalse(accountService.accountExists("MISSING"));
  }
}
