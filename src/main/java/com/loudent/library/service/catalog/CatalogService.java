package com.loudent.library.service.catalog;

import com.google.common.annotations.VisibleForTesting;
import com.loudent.library.api.error.NotFoundException;
import com.loudent.library.aspect.TimedSync;
import com.loudent.library.config.DynamoDbConfig;
import com.loudent.library.dao.activity.Activity;
import com.loudent.library.dao.catalog.Catalog;
import com.loudent.library.dao.catalog.CatalogSearchExpressionBuilder;
import com.loudent.library.oas.codegen.model.CatalogResponse;
import com.loudent.library.oas.codegen.model.CatalogSearchRequest;
import com.loudent.library.service.activity.ActivityService;
import com.loudent.library.util.ConcurrentUtils;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Service
@Log4j2
public class CatalogService {

  private final DynamoDbAsyncTable<Catalog> catalogTable;
  private final ActivityService activityService;
  private final CatalogSearchExpressionBuilder catalogSearchExpressionBuilder;
  private final ExecutorService serviceThreadPool;

  public CatalogService(
      DynamoDbEnhancedAsyncClient client,
      DynamoDbConfig config,
      @Lazy ActivityService activityService,
      CatalogSearchExpressionBuilder catalogSearchExpressionBuilder,
      ExecutorService serviceThreadPool) {
    this.catalogTable =
        client.table(config.getPrefixedTableName("Catalog"), TableSchema.fromBean(Catalog.class));
    this.activityService = activityService;
    this.catalogSearchExpressionBuilder = catalogSearchExpressionBuilder;
    this.serviceThreadPool = serviceThreadPool;
  }

  @TimedSync(
      metric = "getByIsbn",
      tags = {"component:catalog"})
  public CatalogResponse getByIsbn(String isbn) {
    try {
      Catalog catalog = getByIsbnAsync(isbn).join();
      if (catalog == null) {
        throw new NotFoundException("Book not found for ISBN: " + isbn);
      }

      CatalogResponse response = mapToCatalogResponse(catalog);
      return enrichWithAvailability(response, isbn);
    } catch (Exception e) {
      throw new RuntimeException("Failed to retrieve book with ISBN " + isbn, e);
    }
  }

  public CompletableFuture<Catalog> getByIsbnAsync(String isbn) {
    return catalogTable.getItem(requestForIsbn(isbn));
  }

  @TimedSync(
      metric = "getByIsbn",
      tags = {"component:catalog"})
  public CatalogResponse getBookByTitle(String title) {
    try {
      Catalog catalog = getBookByTitleAsync(title).join();
      if (catalog == null) {
        throw new NotFoundException("Book not found for title: " + title);
      }
      CatalogResponse response = mapToCatalogResponse(catalog);
      return enrichWithAvailability(response, catalog.getIsbn());
    } catch (Exception e) {
      throw new RuntimeException("Failed to retrieve book with title " + title, e);
    }
  }

  public CompletableFuture<Catalog> getBookByTitleAsync(String title) {
    Map<String, AttributeValue> expressionValues = new HashMap<>();
    expressionValues.put(":title", AttributeValue.builder().s(title).build());

    Expression expression =
        Expression.builder()
            .expression("title = :title")
            .expressionValues(expressionValues)
            .build();

    ScanEnhancedRequest request =
        ScanEnhancedRequest.builder().filterExpression(expression).build();

    List<Catalog> results = new ArrayList<>();

    CompletableFuture<Void> scanFuture = catalogTable.scan(request).items().subscribe(results::add);

    return scanFuture.thenApply(v -> results.isEmpty() ? null : results.get(0));
  }

  @TimedSync(
      metric = "searchCatalog",
      tags = {"component:catalog"})
  public List<CatalogResponse> search(CatalogSearchRequest request) {
    try {
      List<Catalog> items = searchCatalogAsync(request).join();

      return ConcurrentUtils.parallelMap(
          items,
          item -> {
            CatalogResponse response = mapToCatalogResponse(item);
            return enrichWithAvailability(response, item.getIsbn());
          },
          serviceThreadPool);

    } catch (Exception e) {
      throw new RuntimeException("Failed to perform catalog search", e);
    }
  }

  public CompletableFuture<List<Catalog>> searchCatalogAsync(CatalogSearchRequest request) {
    Expression expression = catalogSearchExpressionBuilder.from(request);
    ScanEnhancedRequest.Builder builder = ScanEnhancedRequest.builder();
    if (expression != null) {
      builder.filterExpression(expression);
    }

    List<Catalog> result = new ArrayList<>();
    return catalogTable.scan(builder.build()).items().subscribe(result::add).thenApply(v -> result);
  }

  @VisibleForTesting
  GetItemEnhancedRequest requestForIsbn(String isbn) {
    return GetItemEnhancedRequest.builder().key(Key.builder().partitionValue(isbn).build()).build();
  }

  @VisibleForTesting
  CatalogResponse mapToCatalogResponse(Catalog catalog) {
    if (catalog == null) return null;

    int totalCopies = Optional.ofNullable(catalog.getBookIds()).map(List::size).orElse(0);

    return new CatalogResponse()
        .isbn(catalog.getIsbn())
        .title(catalog.getTitle())
        .authorFirstName(catalog.getAuthorFirstName())
        .authorLastName(catalog.getAuthorLastName())
        .totalCopies(totalCopies);
  }

  @VisibleForTesting
  CatalogResponse enrichWithAvailability(CatalogResponse response, String isbn) {
    if (response == null || isbn == null) return response;

    List<Activity> checkouts = activityService.getByIsbn(isbn);
    int checkedOut = checkouts != null ? checkouts.size() : 0;
    int available = Math.max(0, response.getTotalCopies() - checkedOut);
    log.debug(
        "Enriched book [{}]: total={}, checkedOut={}, available={}",
        isbn,
        response.getTotalCopies(),
        checkedOut,
        available);
    return response.availableCopies(available);
  }
}
