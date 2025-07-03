package com.loudent.library.model;

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
}
