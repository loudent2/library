package com.loudent.library.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BookData {
  private String isbn;
  private String title;
  private String author;
  private int copies;
}
