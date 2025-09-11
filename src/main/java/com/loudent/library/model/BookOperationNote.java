package com.loudent.library.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum BookOperationNote {
  OK("Ok"),
  ALREADY_CHECKED_IN("Book was already checked in"),
  REPLACED_EXISTING("Book was already checked out, replaced with new record"),
  UNREGISTERED("Book is not registered in the catalog"),
  ERROR("Error");

  private final String message;

  BookOperationNote(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return String.valueOf(message);
  }

  @JsonCreator
  public static BookOperationNote fromValue(String value) {
    for (BookOperationNote book : BookOperationNote.values()) {
      if (book.message.equals(value)) {
        return book;
      }
    }
    return BookOperationNote.ERROR;
  }
}
