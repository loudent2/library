package com.loudent.library;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LibraryApplicationTests {

  @Test
  void contextLoads() {
    assertTrue(true, "Spring Boot context loads successfully.");
  }

  @Test
  void main_shouldStartApplication() {
    assertDoesNotThrow(() -> LibraryApplication.main(new String[] {}));
  }
}
