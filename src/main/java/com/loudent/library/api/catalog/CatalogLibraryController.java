package com.loudent.library.api.catalog;

import com.loudent.library.api.error.ExceptionUtils;
import com.loudent.library.api.error.NotFoundException;
import com.loudent.library.aspect.TimedAsync;
import com.loudent.library.config.LibraryConfig;
import com.loudent.library.oas.codegen.api.CatalogLibrary;
import com.loudent.library.oas.codegen.model.CatalogResponse;
import com.loudent.library.oas.codegen.model.CatalogSearchRequest;
import com.loudent.library.oas.codegen.model.GetBookByTitleRequest;
import com.loudent.library.service.catalog.CatalogService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Log4j2
public class CatalogLibraryController implements CatalogLibrary {
  private final LibraryConfig libraryConfig;
  private final CatalogService catalogService;
  private final ExecutorService controllerThreadPool;

  @Override
  @TimedAsync(
      metric = "getBookByISBN",
      tags = {"component:api"})
  public CompletableFuture<ResponseEntity<CatalogResponse>> getBookByISBN(String isbn) {
    return CompletableFuture.supplyAsync(() -> catalogService.getByIsbn(isbn), controllerThreadPool)
        .orTimeout(libraryConfig.getRequestTimeout(), TimeUnit.MILLISECONDS)
        .handleAsync(
            (response, throwable) -> {
              if (throwable != null) {
                Throwable cause = ExceptionUtils.unwrap(throwable);
                log.error("Problem getting book for isbn# {}", isbn, cause);
                throw new CompletionException(cause);
              }
              if (response == null) {
                throw new CompletionException(
                    new NotFoundException("Book not found for ISBN: " + isbn));
              }
              return ResponseEntity.ok(response);
            },
            controllerThreadPool);
  }

  @Override
  @TimedAsync(
      metric = "getBookByTitle",
      tags = {"component:api"})
  public CompletableFuture<ResponseEntity<CatalogResponse>> getBookByTitle(
      GetBookByTitleRequest titleRequest) {
    return CompletableFuture.supplyAsync(
            () -> catalogService.getBookByTitle(titleRequest.getTitle()), controllerThreadPool)
        .orTimeout(libraryConfig.getRequestTimeout(), TimeUnit.MILLISECONDS)
        .handleAsync(
            (booksResponse, throwable) -> {
              if (throwable != null) {
                log.error("Problem getting book for Title# {}", titleRequest.getTitle(), throwable);
                Throwable cause = ExceptionUtils.unwrap(throwable);
                throw new CompletionException(cause);
              }
              if (null == booksResponse) {
                throw new CompletionException(
                    new NotFoundException(
                        "No catalog entry found for title: " + titleRequest.getTitle()));
              }
              return new ResponseEntity<>(booksResponse, HttpStatus.OK);
            },
            controllerThreadPool);
  }

  @Override
  @TimedAsync(
      metric = "searchCatalog",
      tags = {"component:api"})
  public CompletableFuture<ResponseEntity<List<CatalogResponse>>> searchCatalog(
      CatalogSearchRequest request) {
    return CompletableFuture.supplyAsync(() -> catalogService.search(request), controllerThreadPool)
        .orTimeout(libraryConfig.getRequestTimeout(), TimeUnit.MILLISECONDS)
        .handleAsync(
            (result, throwable) -> {
              if (throwable != null) {
                Throwable cause = ExceptionUtils.unwrap(throwable);
                log.error("Catalog search failed: {}", request, cause);
                throw new CompletionException(cause);
              }

              return ResponseEntity.ok(result);
            },
            controllerThreadPool);
  }
}
