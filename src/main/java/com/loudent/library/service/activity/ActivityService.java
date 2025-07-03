package com.loudent.library.service.activity;

import static com.loudent.library.dao.activity.Activity.ACCOUNT_INDEX;
import static com.loudent.library.dao.activity.Activity.ISBN_INDEX;

import com.google.common.annotations.VisibleForTesting;
import com.loudent.library.config.DynamoDbConfig;
import com.loudent.library.dao.activity.Activity;
import com.loudent.library.dao.catalog.Catalog;
import com.loudent.library.model.BookOperationNote;
import com.loudent.library.oas.codegen.model.BookOperationResult;
import com.loudent.library.service.account.AccountService;
import com.loudent.library.service.catalog.CatalogService;
import com.loudent.library.util.ConcurrentUtils;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Service
@Log4j2
public class ActivityService {
  private final DynamoDbTable<Activity> activityTable;
  private final CatalogService catalogService;
  private final AccountService accountService;
  private final ExecutorService serviceThreadPool;

  public ActivityService(
      DynamoDbEnhancedClient client,
      DynamoDbConfig config,
      CatalogService catalogService,
      AccountService accountService,
      ExecutorService serviceThreadPool) {
    this.activityTable =
        client.table(config.getPrefixedTableName("Activity"), TableSchema.fromBean(Activity.class));
    this.catalogService = catalogService;
    this.serviceThreadPool = serviceThreadPool;
    this.accountService = accountService;
  }

  public Activity getByBookId(String bookId) {
    return activityTable.getItem(Key.builder().partitionValue(bookId).build());
  }

  public List<Activity> getByIsbn(String isbn) {
    DynamoDbIndex<Activity> index = activityTable.index(ISBN_INDEX);
    return collectItems(index.query(QueryConditional.keyEqualTo(k -> k.partitionValue(isbn))));
  }

  public List<Activity> getByAccountNumber(String accountNumber) {
    DynamoDbIndex<Activity> index = activityTable.index(ACCOUNT_INDEX);
    return collectItems(
        index.query(QueryConditional.keyEqualTo(k -> k.partitionValue(accountNumber))));
  }

  public boolean isBookCheckedOut(String bookId) {
    return getByBookId(bookId) != null;
  }

  public List<BookOperationResult> checkoutBooks(String accountNumber, List<String> bookIds) {
    if (accountNumber == null || bookIds == null || bookIds.isEmpty()) {
      throw new IllegalArgumentException("Account number and book IDs must be provided");
    }

    if (!accountService.accountExists(accountNumber)) {
      throw new IllegalArgumentException("Account not found: " + accountNumber);
    }
    return ConcurrentUtils.parallelMap(
        bookIds, bookId -> processCheckout(accountNumber, bookId), serviceThreadPool);
  }

  public List<BookOperationResult> checkinBooks(List<String> bookIds) {
    return ConcurrentUtils.parallelMap(bookIds, this::processCheckin, serviceThreadPool);
  }

  public void deleteByBookId(String bookId) {
    Activity existing = getByBookId(bookId);
    if (existing != null) {
      activityTable.deleteItem(existing);
      log.debug("Deleted checkout activity for bookId: {}", bookId);
    } else {
      log.warn("No checkout record found for bookId: {}", bookId);
    }
  }

  @VisibleForTesting
  BookOperationResult processCheckout(String accountNumber, String bookId) {
    try {
      Catalog catalog = fetchCatalogSafely(bookId).orElse(null);
      if (catalog == null) {
        return new BookOperationResult()
            .bookId(bookId)
            .notes(BookOperationNote.UNREGISTERED.getMessage());
      }

      String title = catalog.getTitle();
      LocalDate now = LocalDate.now();
      LocalDate due = now.plusWeeks(3);

      boolean bBookCheckedOut = isBookCheckedOut(bookId);
      if (bBookCheckedOut) {
        deleteByBookId(bookId);
      }

      Activity activity = new Activity();
      activity.setBookId(bookId);
      activity.setIsbn(extractIsbn(bookId));
      activity.setAccountNumber(accountNumber);
      activity.setCheckOutDate(now);
      activity.setDueDate(due);

      activityTable.putItem(activity);

      return new BookOperationResult()
          .bookId(bookId)
          .title(title)
          .checkOutDate(now)
          .dueByDate(due)
          .notes(
              bBookCheckedOut
                  ? BookOperationNote.REPLACED_EXISTING.getMessage()
                  : BookOperationNote.OK.getMessage());
    } catch (Exception e) {
      log.error("Checkout failed for bookId {}: {}", bookId, e.toString());
      return new BookOperationResult().bookId(bookId).notes("Error: " + e.getMessage());
    }
  }

  @VisibleForTesting
  BookOperationResult processCheckin(String bookId) {
    try {

      Catalog catalog = fetchCatalogSafely(bookId).orElse(null);

      if (catalog == null) {
        return new BookOperationResult()
            .bookId(bookId)
            .notes(BookOperationNote.UNREGISTERED.getMessage());
      }

      String title = catalog.getTitle();
      Activity existing = getByBookId(bookId);

      if (existing != null) {
        deleteByBookId(bookId);
        return new BookOperationResult()
            .bookId(bookId)
            .title(title)
            .checkOutDate(existing.getCheckOutDate())
            .dueByDate(existing.getDueDate())
            .notes(BookOperationNote.OK.getMessage());
      } else {
        return new BookOperationResult()
            .bookId(bookId)
            .notes(BookOperationNote.ALREADY_CHECKED_IN.getMessage());
      }

    } catch (Exception e) {
      log.error("Check in failed for bookId {}: {}", bookId, e.toString());
      return new BookOperationResult().bookId(bookId).notes("Error: " + e.getMessage());
    }
  }

  @VisibleForTesting
  String extractIsbn(String bookId) {
    int dotIndex = bookId.indexOf('.');
    return (dotIndex > 0) ? bookId.substring(0, dotIndex) : bookId;
  }

  @VisibleForTesting
  List<Activity> collectItems(Iterable<Page<Activity>> pages) {
    List<Activity> result = new ArrayList<>(16); // defaulgt dyanamodb return list.
    for (Page<Activity> page : pages) {
      result.addAll(page.items());
    }
    return result;
  }

  @VisibleForTesting
  Optional<Catalog> fetchCatalogSafely(String bookId) {
    String isbn = extractIsbn(bookId);
    try {
      Catalog catalog = catalogService.getByIsbnAsync(isbn).join();
      return Optional.ofNullable(catalog);
    } catch (Exception e) {
      log.warn("Failed to retrieve catalog for bookId {}: {}", bookId, e.toString());
      return Optional.empty();
    }
  }
}
