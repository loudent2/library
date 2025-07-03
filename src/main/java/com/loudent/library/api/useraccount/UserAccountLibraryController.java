package com.loudent.library.api.useraccount;

import com.loudent.library.api.error.ExceptionUtils;
import com.loudent.library.api.error.NotFoundException;
import com.loudent.library.aspect.TimedAsync;
import com.loudent.library.config.LibraryConfig;
import com.loudent.library.oas.codegen.api.UserLibrary;
import com.loudent.library.oas.codegen.model.UserResponse;
import com.loudent.library.service.account.AccountService;
import java.util.concurrent.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Log4j2
@Validated
public class UserAccountLibraryController implements UserLibrary {

  private final LibraryConfig libraryConfig;
  private final AccountService accountService;
  private final ExecutorService controllerThreadPool;

  @Override
  @TimedAsync(
      metric = "getUserByAccountNumber",
      tags = {"component:api"})
  public CompletableFuture<ResponseEntity<UserResponse>> getUserByAccountNumber(
      String accountNumber) {
    return CompletableFuture.supplyAsync(
            () -> accountService.getByAccountNumber(accountNumber), controllerThreadPool)
        .orTimeout(libraryConfig.getRequestTimeout(), TimeUnit.MILLISECONDS)
        .handleAsync(
            (userResponse, throwable) -> {
              if (throwable != null) {
                Throwable cause = ExceptionUtils.unwrap(throwable);
                log.error("Problem getting account #{}", accountNumber, cause);
                throw new CompletionException(cause);
              }

              if (userResponse == null) {
                throw new CompletionException(
                    new NotFoundException("User not found for account #: " + accountNumber));
              }

              return ResponseEntity.ok(userResponse);
            },
            controllerThreadPool);
  }
}
