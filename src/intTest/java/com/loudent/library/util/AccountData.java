package com.loudent.library.util;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AccountData {
  private String accountNumber;
  private String name;
  private LocalDate memberSince;
}
