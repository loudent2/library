package com.loudent.library.api.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.loudent.library.config.LibraryConfig;
import com.loudent.library.oas.codegen.model.CatalogResponse;
import com.loudent.library.oas.codegen.model.GetBookByTitleRequest;
import com.loudent.library.service.catalog.CatalogService;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogLibraryControllerTest {

  private CatalogService catalogService;
  private LibraryConfig libraryConfig;
  private ExecutorService controllerThreadPool;
  private CatalogLibraryController controller;

  @BeforeEach
  void setup() {
    catalogService = mock(CatalogService.class);
    libraryConfig = mock(LibraryConfig.class);
    controllerThreadPool = Executors.newSingleThreadExecutor();
    controller = new CatalogLibraryController(libraryConfig, catalogService, controllerThreadPool);

    when(libraryConfig.getRequestTimeout()).thenReturn(1000L); // 1 second timeout
  }

  @Test
  void getBookByISBN_success() throws Exception {
    String isbn = "1234567890";

    CatalogResponse responseMock = new CatalogResponse().isbn(isbn).title("Test Title");

    when(catalogService.getByIsbn(isbn)).thenReturn(responseMock);

    CatalogResponse result = controller.getBookByISBN(isbn).get().getBody();

    assertNotNull(result);
    assertEquals("1234567890", result.getIsbn());
    assertEquals("Test Title", result.getTitle());
  }

  @Test
  void getBookByISBN_notFound() {
    String isbn = "0000000000";
    when(catalogService.getByIsbn(isbn)).thenReturn(null);

    Exception ex = assertThrows(Exception.class, () -> controller.getBookByISBN(isbn).get());
    assertTrue(ex.getCause().getMessage().contains("Book not found for ISBN"));
  }

  @Test
  void getBookByISBN_serviceThrows_shouldWrapAndPropagate() {
    String isbn = "9999999999";
    when(catalogService.getByIsbn(isbn)).thenThrow(new RuntimeException("Simulated error"));

    Exception ex = assertThrows(Exception.class, () -> controller.getBookByISBN(isbn).get());
    assertTrue(ex.getCause().getMessage().contains("Simulated error"));
  }

  @Test
  void getBookByTitle_success() throws Exception {
    var request = new GetBookByTitleRequest().title("Some Book");
    var response = new CatalogResponse().isbn("123").title("Some Book");

    when(catalogService.getBookByTitle("Some Book")).thenReturn(response);

    CatalogResponse result = controller.getBookByTitle(request).get().getBody();

    assertNotNull(result);
    assertEquals("Some Book", result.getTitle());
  }

  @Test
  void getBookByTitle_notFound() {
    var request = new GetBookByTitleRequest().title("Missing Book");

    when(catalogService.getBookByTitle("Missing Book")).thenReturn(null);

    Exception ex = assertThrows(Exception.class, () -> controller.getBookByTitle(request).get());
    assertTrue(ex.getCause().getMessage().contains("No catalog entry found for title"));
  }

  @Test
  void getBookByTitle_serviceThrows_shouldWrapAndPropagate() {
    var request = new GetBookByTitleRequest().title("Error Book");

    when(catalogService.getBookByTitle("Error Book"))
        .thenThrow(new RuntimeException("Simulated failure"));

    Exception ex = assertThrows(Exception.class, () -> controller.getBookByTitle(request).get());
    assertTrue(ex.getCause().getMessage().contains("Simulated failure"));
  }

  @Test
  void searchCatalog_success() throws Exception {
    var request = new com.loudent.library.oas.codegen.model.CatalogSearchRequest();
    var resultList = List.of(new CatalogResponse().isbn("123").title("Search Result"));

    when(catalogService.search(request)).thenReturn(resultList);

    List<CatalogResponse> results = controller.searchCatalog(request).get().getBody();

    assertNotNull(results);
    assertEquals(1, results.size());
    assertEquals("Search Result", results.get(0).getTitle());
  }

  @Test
  void searchCatalog_serviceThrows_shouldWrapAndPropagate() {
    var request = new com.loudent.library.oas.codegen.model.CatalogSearchRequest();

    when(catalogService.search(request)).thenThrow(new RuntimeException("Search failed"));

    Exception ex = assertThrows(Exception.class, () -> controller.searchCatalog(request).get());
    assertTrue(ex.getCause().getMessage().contains("Search failed"));
  }
}
