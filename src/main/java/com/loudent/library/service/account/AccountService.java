package com.loudent.library.service.account;

import com.google.common.annotations.VisibleForTesting;
import com.loudent.library.api.error.NotFoundException;
import com.loudent.library.config.DynamoDbConfig;
import com.loudent.library.dao.account.Account;
import com.loudent.library.dao.activity.Activity;
import com.loudent.library.oas.codegen.model.BorrowedBook;
import com.loudent.library.oas.codegen.model.UserResponse;
import com.loudent.library.service.activity.ActivityService;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;

@Service
@Log4j2
public class AccountService {

  private final DynamoDbAsyncTable<Account> accountTable;
  private final ActivityService activityService;

  public AccountService(
      DynamoDbEnhancedAsyncClient client,
      DynamoDbConfig config,
      @Lazy ActivityService activityService) {
    this.accountTable =
        client.table(config.getPrefixedTableName("Accounts"), TableSchema.fromBean(Account.class));
    this.activityService = activityService;
  }

  // Blocking
  public UserResponse getByAccountNumber(String accountNumber) {
    try {
      Account account = getByAccountNumberAsync(accountNumber).join();
      if (account == null) {
        throw new NotFoundException("Account not found for account number: " + accountNumber);
      }

      UserResponse response = mapToUserResponse(account);
      return enrichWithBorrowedBooks(response, accountNumber);
    } catch (Exception e) {
      throw new RuntimeException("Failed to retrieve account #" + accountNumber, e);
    }
  }

  public boolean accountExists(String accountNumber) {
    try {
      Account account = getByAccountNumberAsync(accountNumber).join();
      return account != null;
    } catch (Exception e) {
      log.debug("Account lookup failed for {}: {}", accountNumber, e.toString());
    }
    return false;
  }

  // Async
  public CompletableFuture<Account> getByAccountNumberAsync(String accountNumber) {
    return accountTable.getItem(requestForAccount(accountNumber));
  }

  /** Builds a DynamoDB GetItemEnhancedRequest with the partition key for the account. */
  @VisibleForTesting
  GetItemEnhancedRequest requestForAccount(String accountNumber) {
    return GetItemEnhancedRequest.builder()
        .key(Key.builder().partitionValue(accountNumber).build())
        .build();
  }

  @VisibleForTesting
  UserResponse mapToUserResponse(Account account) {
    if (account == null) return null;
    return new UserResponse()
        .accountNumber(account.getAccountNumber())
        .firstName(account.getFirstName())
        .lastName(account.getLastName())
        .memberSince(account.getMemberSince());
  }

  @VisibleForTesting
  UserResponse enrichWithBorrowedBooks(UserResponse response, String accountNumber) {
    List<Activity> checkouts = activityService.getByAccountNumber(accountNumber);
    List<BorrowedBook> borrowedBooks =
        (checkouts != null ? checkouts : Collections.<Activity>emptyList())
            .stream().map(this::mapToBorrowedBook).collect(Collectors.toList());
    return response.borrowedBooks(borrowedBooks);
  }

  @VisibleForTesting
  BorrowedBook mapToBorrowedBook(Activity activity) {
    return new BorrowedBook()
        .bookId(activity.getBookId())
        .title(activity.getTitle())
        .checkOutDate(activity.getCheckOutDate())
        .dueByDate(activity.getDueDate());
  }
}
