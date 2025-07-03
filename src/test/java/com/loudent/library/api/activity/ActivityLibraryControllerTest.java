package com.loudent.library.api.activity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.loudent.library.config.LibraryConfig;
import com.loudent.library.oas.codegen.model.BookOperationResult;
import com.loudent.library.oas.codegen.model.CheckinRequest;
import com.loudent.library.oas.codegen.model.CheckoutRequest;
import com.loudent.library.service.activity.ActivityService;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActivityLibraryControllerTest {

  private ActivityService activityService;
  private LibraryConfig libraryConfig;
  private ExecutorService controllerThreadPool;
  private ActivityLibraryController controller;

  @BeforeEach
  void setup() {
    activityService = mock(ActivityService.class);
    libraryConfig = mock(LibraryConfig.class);
    controllerThreadPool = Executors.newSingleThreadExecutor();
    controller =
        new ActivityLibraryController(libraryConfig, activityService, controllerThreadPool);

    when(libraryConfig.getRequestTimeout()).thenReturn(1000L); // 1 second timeout
  }

  // === CHECKOUT TESTS ===

  @Test
  void checkoutBooks_success() throws Exception {
    CheckoutRequest request =
        new CheckoutRequest().accountNumber("acct1").bookIds(List.of("isbn.copy1", "isbn.copy2"));

    List<BookOperationResult> mockResults =
        List.of(
            new BookOperationResult().bookId("isbn.copy1"),
            new BookOperationResult().bookId("isbn.copy2"));

    when(activityService.checkoutBooks("acct1", request.getBookIds())).thenReturn(mockResults);

    List<BookOperationResult> result = controller.checkoutBooks(request).get().getBody();

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("isbn.copy1", result.get(0).getBookId());
  }

  @Test
  void checkoutBooks_serviceThrows_shouldWrapAndPropagate() {
    CheckoutRequest request =
        new CheckoutRequest().accountNumber("acct2").bookIds(List.of("isbn.copy1"));

    when(activityService.checkoutBooks("acct2", request.getBookIds()))
        .thenThrow(new RuntimeException("Simulated checkout failure"));

    Exception ex = assertThrows(Exception.class, () -> controller.checkoutBooks(request).get());
    assertTrue(ex.getCause().getMessage().contains("Simulated checkout failure"));
  }

  // === CHECKIN TESTS ===

  @Test
  void checkinBooks_success() throws Exception {
    CheckinRequest request = new CheckinRequest().bookIds(List.of("isbn.copy1"));

    List<BookOperationResult> mockResults = List.of(new BookOperationResult().bookId("isbn.copy1"));

    when(activityService.checkinBooks(request.getBookIds())).thenReturn(mockResults);

    List<BookOperationResult> result = controller.checkinBooks(request).get().getBody();

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("isbn.copy1", result.get(0).getBookId());
  }

  @Test
  void checkinBooks_serviceThrows_shouldWrapAndPropagate() {
    CheckinRequest request = new CheckinRequest().bookIds(List.of("isbn.copy1"));

    when(activityService.checkinBooks(request.getBookIds()))
        .thenThrow(new RuntimeException("Simulated checkin failure"));

    Exception ex = assertThrows(Exception.class, () -> controller.checkinBooks(request).get());
    assertTrue(ex.getCause().getMessage().contains("Simulated checkin failure"));
  }
}
