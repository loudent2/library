package com.loudent.library.util;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ActivityData {
  private String bookId;
  private String isbn;
  private String title;
  private String accountNumber;
  private LocalDate checkOutDate;
  private LocalDate dueDate;
}
