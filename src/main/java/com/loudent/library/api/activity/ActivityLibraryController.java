package com.loudent.library.api.activity;

import com.loudent.library.api.error.ExceptionUtils;
import com.loudent.library.aspect.TimedAsync;
import com.loudent.library.config.LibraryConfig;
import com.loudent.library.oas.codegen.api.ActivityLibrary;
import com.loudent.library.oas.codegen.model.BookOperationResult;
import com.loudent.library.oas.codegen.model.CheckinRequest;
import com.loudent.library.oas.codegen.model.CheckoutRequest;
import com.loudent.library.service.activity.ActivityService;
import java.util.List;
import java.util.concurrent.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Log4j2
public class ActivityLibraryController implements ActivityLibrary {

  private final LibraryConfig libraryConfig;
  private final ActivityService activityService;
  private final ExecutorService controllerThreadPool;

  @Override
  @TimedAsync(
      metric = "checkoutBooks",
      tags = {"component:api"})
  public CompletableFuture<ResponseEntity<List<BookOperationResult>>> checkoutBooks(
      CheckoutRequest request) {
    return CompletableFuture.supplyAsync(
            () -> activityService.checkoutBooks(request.getAccountNumber(), request.getBookIds()),
            controllerThreadPool)
        .orTimeout(libraryConfig.getRequestTimeout(), TimeUnit.MILLISECONDS)
        .handleAsync(
            (result, throwable) -> {
              if (throwable != null) {
                Throwable cause = ExceptionUtils.unwrap(throwable);
                log.error(
                    "Checkout failed for account {}: {}",
                    request.getAccountNumber(),
                    cause.toString());
                throw new CompletionException(cause);
              }

              return ResponseEntity.ok(result);
            },
            controllerThreadPool);
  }

  @Override
  @TimedAsync(
      metric = "checkinBooks",
      tags = {"component:api"})
  public CompletableFuture<ResponseEntity<List<BookOperationResult>>> checkinBooks(
      CheckinRequest request) {
    return CompletableFuture.supplyAsync(
            () -> activityService.checkinBooks(request.getBookIds()), controllerThreadPool)
        .orTimeout(libraryConfig.getRequestTimeout(), TimeUnit.MILLISECONDS)
        .handleAsync(
            (result, throwable) -> {
              if (throwable != null) {
                Throwable cause = ExceptionUtils.unwrap(throwable);
                log.error(
                    "Checkin failed for bookIds {}: {}", request.getBookIds(), cause.toString());
                throw new CompletionException(cause);
              }

              return ResponseEntity.ok(result);
            },
            controllerThreadPool);
  }
}
