package com.loudent.library.api.useraccount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.loudent.library.api.error.NotFoundException;
import com.loudent.library.config.LibraryConfig;
import com.loudent.library.oas.codegen.model.UserResponse;
import com.loudent.library.service.account.AccountService;
import java.time.LocalDate;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class UserAccountLibraryControllerTest {

  @InjectMocks private UserAccountLibraryController controller;

  @Mock private AccountService accountService;

  @Mock private LibraryConfig libraryConfig;

  @Mock private ExecutorService controllerThreadPool;

  private UserResponse sampleUser;

  @BeforeEach
  void setup() {
    sampleUser =
        new UserResponse()
            .accountNumber("ACC123456")
            .firstName("Alice")
            .lastName("Smith")
            .memberSince(LocalDate.of(2020, 1, 15));
    controllerThreadPool = Executors.newSingleThreadExecutor();
    controller =
        new UserAccountLibraryController(libraryConfig, accountService, controllerThreadPool);
    when(libraryConfig.getRequestTimeout()).thenReturn(2000L);
  }

  @Test
  @SneakyThrows
  void getUserByAccountNumber_success() {
    when(accountService.getByAccountNumber("ACC123456")).thenReturn(sampleUser);

    ResponseEntity<UserResponse> result =
        controller.getUserByAccountNumber("ACC123456").get(3, TimeUnit.SECONDS);

    assertEquals(200, result.getStatusCode().value());
    assertEquals(sampleUser, result.getBody());
  }

  @Test
  @SneakyThrows
  void getUserByAccountNumber_shouldThrowException_whenServiceFails() {
    when(accountService.getByAccountNumber("BROKEN")).thenThrow(new RuntimeException("boom"));

    CompletionException ex =
        assertThrows(
            CompletionException.class, () -> controller.getUserByAccountNumber("BROKEN").join());

    assertEquals("boom", ex.getCause().getMessage());
  }

  @Test
  @SneakyThrows
  void getUserByAccountNumber_shouldThrowNotFound_whenUserIsNull() {
    when(accountService.getByAccountNumber("MISSING")).thenReturn(null);

    CompletionException ex =
        assertThrows(
            CompletionException.class, () -> controller.getUserByAccountNumber("MISSING").join());

    assertInstanceOf(NotFoundException.class, ex.getCause());
    assertEquals("User not found for account #: MISSING", ex.getCause().getMessage());
  }
}
