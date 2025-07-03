package com.loudent.library.service.activity;

import static com.loudent.library.dao.activity.Activity.ACCOUNT_INDEX;
import static com.loudent.library.dao.activity.Activity.ISBN_INDEX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loudent.library.config.DynamoDbConfig;
import com.loudent.library.dao.activity.Activity;
import com.loudent.library.dao.catalog.Catalog;
import com.loudent.library.model.BookOperationNote;
import com.loudent.library.oas.codegen.model.BookOperationResult;
import com.loudent.library.service.account.AccountService;
import com.loudent.library.service.catalog.CatalogService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

class ActivityServiceTest {

  @Mock private DynamoDbEnhancedClient enhancedClient;
  @Mock private DynamoDbConfig config;
  @Mock private CatalogService catalogService;
  @Mock private AccountService accountService;
  @Mock private DynamoDbTable<Activity> activityTable;
  @Mock private DynamoDbIndex<Activity> isbnIndex;
  @Mock private DynamoDbIndex<Activity> accountIndex;

  @Captor private ArgumentCaptor<Activity> activityCaptor;

  private ExecutorService executor;
  private ActivityService service;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    when(config.getPrefixedTableName("Activity")).thenReturn("Activity");
    when(enhancedClient.table(eq("Activity"), ArgumentMatchers.<TableSchema<Activity>>any()))
        .thenReturn(activityTable);
    when(accountService.accountExists(any())).thenReturn(true);
    executor = Executors.newFixedThreadPool(2);
    service = new ActivityService(enhancedClient, config, catalogService, accountService, executor);
  }

  @Test
  void extractIsbn_shouldExtractCorrectly() {
    assertEquals("1234567890", service.extractIsbn("1234567890.abc123"));
    assertEquals("onlyisbn", service.extractIsbn("onlyisbn"));
  }

  @SuppressWarnings("unchecked")
  @Test
  void getByIsbn_shouldReturnActivities() {
    when(activityTable.index(ISBN_INDEX)).thenReturn(isbnIndex);
    Page<Activity> page = mock(Page.class);
    when(page.items()).thenReturn(List.of(new Activity()));
    when(isbnIndex.query(any(QueryConditional.class))).thenReturn(() -> List.of(page).iterator());

    List<Activity> results = service.getByIsbn("isbn123");
    assertEquals(1, results.size());
  }

  @SuppressWarnings("unchecked")
  @Test
  void getByAccount_shouldReturnActivities() {
    when(activityTable.index(ACCOUNT_INDEX)).thenReturn(accountIndex);
    Page<Activity> page = mock(Page.class);
    when(page.items()).thenReturn(List.of(new Activity()));
    when(accountIndex.query(any(QueryConditional.class)))
        .thenReturn(() -> List.of(page).iterator());

    List<Activity> results = service.getByAccountNumber("acct123");
    assertEquals(1, results.size());
  }

  @Test
  void processCheckout_unregisteredBook_shouldReturnUnregisteredNote() {
    when(catalogService.getByIsbnAsync("isbn")).thenReturn(CompletableFuture.completedFuture(null));

    BookOperationResult result = service.processCheckout("acct1", "isbn.copy123");

    assertEquals("isbn.copy123", result.getBookId());
    assertEquals(BookOperationNote.UNREGISTERED.getMessage(), result.getNotes());
    verify(activityTable, never()).putItem(any(Activity.class));
  }

  @Test
  void processCheckin_bookNotCheckedOut_shouldReturnAlreadyCheckedIn() {
    when(catalogService.getByIsbnAsync("isbn"))
        .thenReturn(CompletableFuture.completedFuture(mock(Catalog.class)));
    when(activityTable.getItem(any(Activity.class))).thenReturn(null);

    BookOperationResult result = service.processCheckin("isbn.copy123");

    assertEquals("isbn.copy123", result.getBookId());
    assertEquals(BookOperationNote.ALREADY_CHECKED_IN.getMessage(), result.getNotes());
    verify(activityTable, never()).deleteItem(any(Activity.class));
  }

  @Test
  void processCheckout_existingCheckout_shouldReplaceAndNote() {
    String bookId = "isbn.123";
    String account = "acctA";
    Catalog catalog = mock(Catalog.class);
    when(catalog.getTitle()).thenReturn("Title A");
    when(catalogService.getByIsbnAsync("isbn"))
        .thenReturn(CompletableFuture.completedFuture(catalog));

    // Correct the stubbing for getItem(Key)
    when(activityTable.getItem(any(Key.class))).thenReturn(new Activity());

    BookOperationResult result = service.processCheckout(account, bookId);

    assertEquals(bookId, result.getBookId());
    assertEquals("Title A", result.getTitle());
    assertEquals(BookOperationNote.REPLACED_EXISTING.getMessage(), result.getNotes());
    verify(activityTable).putItem(any(Activity.class));
  }

  @Test
  void getByBookId_shouldReturnActivity() {
    Activity activity = new Activity();
    when(activityTable.getItem(any(Key.class))).thenReturn(activity);
    assertEquals(activity, service.getByBookId("some.bookId"));
  }

  @Test
  void isBookCheckedOut_shouldReturnTrueWhenItemExists() {
    when(activityTable.getItem(any(Key.class))).thenReturn(new Activity());
    assertTrue(service.isBookCheckedOut("some.bookId"));
  }

  @Test
  void isBookCheckedOut_shouldReturnFalseWhenItemMissing() {
    when(activityTable.getItem(any(Key.class))).thenReturn(null);
    assertFalse(service.isBookCheckedOut("some.bookId"));
  }

  @Test
  void checkoutBooks_shouldThrowIfInvalidArgs() {
    IllegalArgumentException ex1 =
        assertThrows(
            IllegalArgumentException.class, () -> service.checkoutBooks(null, List.of("book")));
    assertEquals("Account number and book IDs must be provided", ex1.getMessage());

    IllegalArgumentException ex2 =
        assertThrows(IllegalArgumentException.class, () -> service.checkoutBooks("acct", null));
    assertEquals("Account number and book IDs must be provided", ex2.getMessage());

    IllegalArgumentException ex3 =
        assertThrows(
            IllegalArgumentException.class, () -> service.checkoutBooks("acct", List.of()));
    assertEquals("Account number and book IDs must be provided", ex3.getMessage());
  }

  @Test
  void checkinBooks_shouldReturnResultsForAll() {
    Catalog catalog = mock(Catalog.class);
    when(catalog.getTitle()).thenReturn("Test Book");
    when(catalogService.getByIsbnAsync(any()))
        .thenReturn(CompletableFuture.completedFuture(catalog));
    when(activityTable.getItem(any(Key.class))).thenReturn(new Activity());

    List<BookOperationResult> results = service.checkinBooks(List.of("isbn.copy1", "isbn.copy2"));
    assertEquals(2, results.size());
  }

  @Test
  void deleteByBookId_shouldDeleteIfPresent() {
    Activity activity = new Activity();
    when(activityTable.getItem(any(Key.class))).thenReturn(activity);
    service.deleteByBookId("book123");
    verify(activityTable).deleteItem(activity);
  }

  @Test
  void deleteByBookId_shouldLogWarningIfNotPresent() {
    when(activityTable.getItem(any(Key.class))).thenReturn(null);
    service.deleteByBookId("book123");
    verify(activityTable, never()).deleteItem(any(Activity.class));
  }

  @Test
  void processCheckin_bookCheckedOut_shouldReturnSuccess() {
    Catalog catalog = mock(Catalog.class);
    when(catalog.getTitle()).thenReturn("Book Title");
    when(catalogService.getByIsbnAsync(any()))
        .thenReturn(CompletableFuture.completedFuture(catalog));
    Activity activity = new Activity();
    activity.setCheckOutDate(LocalDate.now().minusDays(3));
    activity.setDueDate(LocalDate.now().plusWeeks(1));
    when(activityTable.getItem(any(Key.class))).thenReturn(activity);

    BookOperationResult result = service.processCheckin("isbn.copy123");

    assertEquals("Book Title", result.getTitle());
    assertEquals(BookOperationNote.OK.getMessage(), result.getNotes());
  }

  @Test
  void checkoutBooks_shouldThrowIfAccountMissing() {
    when(accountService.accountExists("acctX")).thenReturn(false);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> service.checkoutBooks("acctX", List.of("book.1")));
    assertEquals("Account not found: acctX", ex.getMessage());
  }

  @Test
  void checkoutBooks_shouldProcessAllBooks() {
    when(accountService.accountExists("acctA")).thenReturn(true);
    Catalog catalog = mock(Catalog.class);
    when(catalog.getTitle()).thenReturn("Test Book");
    when(catalogService.getByIsbnAsync(any()))
        .thenReturn(CompletableFuture.completedFuture(catalog));
    when(activityTable.getItem(any(Key.class))).thenReturn(null); // book not previously checked out

    List<String> bookIds = List.of("isbn.book1", "isbn.book2");
    List<BookOperationResult> results = service.checkoutBooks("acctA", bookIds);

    assertEquals(2, results.size());
    assertEquals("isbn.book1", results.get(0).getBookId());
    assertEquals("isbn.book2", results.get(1).getBookId());
  }

  @Test
  void checkinBooks_shouldHandleBooksNotCheckedOut() {
    Catalog catalog = mock(Catalog.class);
    when(catalog.getTitle()).thenReturn("Test Book");
    when(catalogService.getByIsbnAsync(any()))
        .thenReturn(CompletableFuture.completedFuture(catalog));
    when(activityTable.getItem(any(Key.class))).thenReturn(null); // book not checked out

    List<BookOperationResult> results = service.checkinBooks(List.of("isbn.copy1"));
    assertEquals(1, results.size());
    assertEquals(BookOperationNote.ALREADY_CHECKED_IN.getMessage(), results.get(0).getNotes());
  }

  @Test
  void checkinBooks_shouldHandleCatalogServiceException() {
    when(catalogService.getByIsbnAsync(any())).thenThrow(new RuntimeException("Simulated failure"));

    List<BookOperationResult> results = service.checkinBooks(List.of("isbn.copy1"));

    assertEquals(1, results.size());
    assertEquals(results.get(0).getNotes(), BookOperationNote.UNREGISTERED.getMessage());
  }

  @Test
  void processCheckin_shouldHandleCatalogServiceException() {
    when(catalogService.getByIsbnAsync(any())).thenThrow(new RuntimeException("Simulated failure"));

    BookOperationResult result = service.processCheckin("isbn.copyX");
    assertEquals(result.getNotes(), BookOperationNote.UNREGISTERED.getMessage());
  }

  @Test
  void fetchCatalogSafely_shouldHandleException() {
    CompletableFuture<Catalog> failingFuture = new CompletableFuture<>();
    failingFuture.completeExceptionally(new RuntimeException("Catalog error"));

    when(catalogService.getByIsbnAsync(any())).thenReturn(failingFuture);

    Optional<Catalog> result = service.fetchCatalogSafely("isbn.copyX");

    assertTrue(result.isEmpty());
  }

  @Test
  void processCheckout_shouldHandlePutItemException() {
    Catalog catalog = mock(Catalog.class);
    when(catalog.getTitle()).thenReturn("Some Title");
    when(catalogService.getByIsbnAsync(any()))
        .thenReturn(CompletableFuture.completedFuture(catalog));

    // Simulate putItem throwing an exception
    doThrow(new RuntimeException("putItem failed"))
        .when(activityTable)
        .putItem(any(Activity.class));

    BookOperationResult result = service.processCheckout("acct1", "isbn.copy123");

    assertEquals("isbn.copy123", result.getBookId());
    assertTrue(result.getNotes().startsWith("Error:"));
  }

  @Test
  void processCheckin_shouldHandleDeleteException() {
    Catalog catalog = mock(Catalog.class);
    when(catalog.getTitle()).thenReturn("Some Title");
    when(catalogService.getByIsbnAsync(any()))
        .thenReturn(CompletableFuture.completedFuture(catalog));

    Activity activity = new Activity();
    activity.setCheckOutDate(LocalDate.now().minusDays(3));
    activity.setDueDate(LocalDate.now().plusWeeks(3));

    when(activityTable.getItem(any(Key.class))).thenReturn(activity);

    // Simulate deleteItem throwing an exception
    doThrow(new RuntimeException("deleteItem failed"))
        .when(activityTable)
        .deleteItem(any(Activity.class));

    BookOperationResult result = service.processCheckin("isbn.copy123");

    assertEquals("isbn.copy123", result.getBookId());
    assertTrue(result.getNotes().startsWith("Error:"));
  }
}
