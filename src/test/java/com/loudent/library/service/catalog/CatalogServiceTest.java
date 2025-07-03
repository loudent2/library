package com.loudent.library.service.catalog;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.loudent.library.api.error.NotFoundException;
import com.loudent.library.config.DynamoDbConfig;
import com.loudent.library.dao.activity.Activity;
import com.loudent.library.dao.catalog.Catalog;
import com.loudent.library.dao.catalog.CatalogSearchExpressionBuilder;
import com.loudent.library.oas.codegen.model.CatalogResponse;
import com.loudent.library.oas.codegen.model.CatalogSearchRequest;
import com.loudent.library.service.activity.ActivityService;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

  @Mock DynamoDbConfig config;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  DynamoDbAsyncTable<Catalog> catalogTable;

  @Mock ActivityService activityService;

  @Mock CatalogSearchExpressionBuilder expressionBuilder;

  @Mock DynamoDbEnhancedAsyncClient client;

  @Mock SdkPublisher<Catalog> mockItemPublisher;

  @InjectMocks CatalogService service;
  private ExecutorService executor;
  Catalog catalog;

  @BeforeEach
  void setup() {
    executor = Executors.newSingleThreadExecutor();
    catalog = new Catalog();
    catalog.setIsbn("1234567890123");
    catalog.setTitle("Test Book");
    catalog.setAuthorFirstName("John");
    catalog.setAuthorLastName("Doe");
    catalog.setBookIds(List.of("1", "2", "3"));
    when(config.getPrefixedTableName("Catalog")).thenReturn("Catalog");
    when(client.table(eq("Catalog"), any(TableSchema.class))).thenReturn(catalogTable);
    service = new CatalogService(client, config, activityService, expressionBuilder, executor);
  }

  @Test
  void getByIsbn_shouldReturnEnrichedResponse() {
    when(catalogTable.getItem(any(GetItemEnhancedRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(catalog));
    when(activityService.getByIsbn("1234567890123")).thenReturn(List.of(new Activity()));

    CatalogResponse response = service.getByIsbn("1234567890123");

    assertEquals("Test Book", response.getTitle());
    assertEquals(3, response.getTotalCopies());
    assertEquals(2, response.getAvailableCopies());
  }

  @Test
  void getByIsbn_shouldThrowNotFound_ifNull() {
    when(catalogTable.getItem(any(GetItemEnhancedRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(null));

    RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getByIsbn("notfound"));
    assertInstanceOf(NotFoundException.class, ex.getCause());
  }

  @Test
  void getBookByTitle_shouldReturnEnrichedResponse() {
    mockScanWithResult(List.of(catalog));
    when(activityService.getByIsbn("1234567890123")).thenReturn(Collections.emptyList());

    CatalogResponse response = service.getBookByTitle("Test Book");

    assertEquals("1234567890123", response.getIsbn());
  }

  @Test
  void getBookByTitle_shouldThrowNotFound_ifNoMatch() {
    mockScanWithResult(List.of());

    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> service.getBookByTitle("Unknown"));
    assertInstanceOf(NotFoundException.class, ex.getCause());
  }

  @SuppressWarnings("unchecked")
  @Test
  void search_shouldReturnMappedAndEnrichedResponses() {
    CatalogSearchRequest request = new CatalogSearchRequest().authorLastName("Doe");
    when(expressionBuilder.from(request)).thenReturn(null); // no filters
    mockScanWithResult(List.of(catalog));

    when(activityService.getByIsbn(any())).thenReturn(List.of(new Activity()));

    List<CatalogResponse> results = service.search(request);

    assertEquals(1, results.size());
    assertEquals(2, results.get(0).getAvailableCopies());
  }

  @Test
  void searchCatalogAsync_shouldBuildAndExecuteScan() {
    CatalogSearchRequest request = new CatalogSearchRequest().authorLastName("Doe");
    Expression expr = Expression.builder().expression("dummy").expressionValues(Map.of()).build();

    when(expressionBuilder.from(request)).thenReturn(expr);

    mockScanWithResult(List.of(catalog));

    List<Catalog> results = service.searchCatalogAsync(request).join();
    assertEquals(1, results.size());
    assertEquals("Test Book", results.get(0).getTitle());
  }

  @Test
  void enrichWithAvailability_shouldCalculateCorrectAvailableCopies() {
    CatalogResponse response =
        new CatalogResponse().isbn("1234567890123").title("Test Book").totalCopies(5);

    when(activityService.getByIsbn("1234567890123"))
        .thenReturn(List.of(new Activity(), new Activity()));

    CatalogResponse enriched = service.enrichWithAvailability(response, "1234567890123");

    assertEquals(3, enriched.getAvailableCopies());
  }

  @Test
  void mapToCatalogResponse_shouldHandleNullBookIds() {
    catalog.setBookIds(null);

    CatalogResponse result = service.mapToCatalogResponse(catalog);
    assertEquals(0, result.getTotalCopies());
  }

  @Test
  void requestForIsbn_shouldBuildCorrectKey() {
    GetItemEnhancedRequest request = service.requestForIsbn("1234567890123");
    assertNotNull(request.key());
    assertEquals("1234567890123", request.key().partitionKeyValue().s());
  }

  @Test
  void searchCatalogAsync_shouldWorkWithoutExpression() {
    CatalogSearchRequest request = new CatalogSearchRequest().authorLastName("Doe");

    when(expressionBuilder.from(request)).thenReturn(null);
    mockScanWithResult(List.of(catalog));

    List<Catalog> results = service.searchCatalogAsync(request).join();

    assertEquals(1, results.size());
    assertEquals("Test Book", results.get(0).getTitle());
  }

  @Test
  void mapToCatalogResponse_shouldReturnNullIfInputNull() {
    assertNull(service.mapToCatalogResponse(null));
  }

  @Test
  void enrichWithAvailability_shouldReturnUnchangedIfIsbnNull() {
    CatalogResponse base = new CatalogResponse().title("Book").totalCopies(3);
    CatalogResponse result = service.enrichWithAvailability(base, null);

    assertSame(base, result); // unchanged
    assertEquals(3, result.getTotalCopies());
  }

  @Test
  void search_shouldThrowIfSearchCatalogFails() {
    CatalogSearchRequest request = new CatalogSearchRequest().authorLastName("Doe");

    CompletableFuture<List<Catalog>> failingFuture = new CompletableFuture<>();
    failingFuture.completeExceptionally(new RuntimeException("Simulated search failure"));

    CatalogService spyService = Mockito.spy(service);
    doReturn(failingFuture).when(spyService).searchCatalogAsync(request);

    RuntimeException ex = assertThrows(RuntimeException.class, () -> spyService.search(request));
    assertTrue(ex.getMessage().contains("Failed to perform catalog search"));
  }

  @Test
  void enrichWithAvailability_shouldReturnNullIfResponseNull() {
    assertNull(service.enrichWithAvailability(null, "1234567890123"));
  }

  @Test
  void enrichWithAvailability_shouldHandleNullCheckouts() {
    CatalogResponse response =
        new CatalogResponse().isbn("1234567890123").title("Book").totalCopies(3);

    when(activityService.getByIsbn("1234567890123")).thenReturn(null);

    CatalogResponse enriched = service.enrichWithAvailability(response, "1234567890123");

    assertEquals(3, enriched.getAvailableCopies()); // 3 - 0
  }

  private void mockScanWithResult(List<Catalog> items) {
    when(catalogTable.scan(any(ScanEnhancedRequest.class)).items())
        .thenAnswer(
            invocation ->
                new SdkPublisher<Catalog>() {
                  @Override
                  public void subscribe(
                      org.reactivestreams.Subscriber<? super Catalog> subscriber) {
                    subscriber.onSubscribe(
                        new org.reactivestreams.Subscription() {
                          private int currentIndex = 0;
                          private boolean cancelled = false;

                          @Override
                          public void request(long n) {
                            if (cancelled) return;

                            int sent = 0;
                            while (sent < n && currentIndex < items.size()) {
                              subscriber.onNext(items.get(currentIndex++));
                              sent++;
                            }

                            if (currentIndex >= items.size()) {
                              subscriber.onComplete();
                            }
                          }

                          @Override
                          public void cancel() {
                            cancelled = true;
                          }
                        });
                  }
                });
  }
}
