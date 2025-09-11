@flow
Feature: Checkin books
  Background:
    Given the service is running
    And the following accounts exist:
      | accountNumber | name          | memberSince |
      | ACC123456     | Alice Smith   | 2020-01-15  |
      | ACC789012     | Bob Johnson   | 2022-09-10  |
      | ACC345678     | Carol Miller  | 2021-03-20  |
    And the following books exist:
      | isbn          | title                | author       | copies |
      | 9781234567890 | Sample Book          | John Doe     | 3      |
      | 9781111111111 | Data Structures      | John Doe     | 5      |
      | 9782222222222 | Machine Learning 101 | Carlos Lopez | 4      |
    And the following book ids exist
      | bookId          | isbn          | accountNumber | title             | dueDate     | checkOutDate |
      | 9781234567890.1 | 9781234567890 | ACC123456     | Sample Book       | 2025-01-15  | 2024-12-15   |
      | 9781111111111.2 | 9781111111111 | ACC123456     | Data Structures   | 2025-01-15  | 2024-12-15   |
      | 9782222222222.1 | 9782222222222 | ACC789012     | Data Structures   | 2025-02-15  | 2025-01-15   |

  Scenario: Checkin returns OK and frees catalog copies
    When I checkin books:
      | bookId          |
      | 9781234567890.1 |
      | 9782222222222.1 |
    Then the activity operation notes should be:
      | bookId               | note |
      | 9781234567890.1      | Ok   |
      | 9782222222222.1      | Ok   |
    And catalog availability should be:
      | isbn          | available |
      | 9781234567890 | 3         |
      | 9782222222222 | 4         |

  Scenario: Checkin of an already-checked-in book returns ALREADY_CHECKED_IN
    Given I checkin books:
      | bookId          |
      | 9781234567890.1 |
    When I checkin books:
      | bookId          |
      | 9781234567890.1 |
    Then the activity operation notes should be:
      | bookId          | note                         |
      | 9781234567890.1 | Book was already checked in  |

  Scenario: Unregistered bookId during checkin does not affect others
    When I checkin books:
      | bookId                 |
      | 9999999999999.ghost    |
      | 9781234567890.1        |
    Then the activity operation notes should be:
      | bookId              | note                                   |
      | 9999999999999.ghost | Book is not registered in the catalog  |
      | 9781234567890.1     | Ok                                     |
    And catalog availability should be:
      | isbn          | available |
      | 9781234567890 | 3         |
