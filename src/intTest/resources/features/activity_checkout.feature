@flow
Feature: Checkout books

  Background:
    Given the service is running
    And the following accounts exist:
      | accountNumber   | name          | memberSince |
      | ACC123456       | Alice Smith   | 2020-01-15  |
      | ACC789012       | Bob Johnson   | 2022-09-10  |
      | ACC345678       | Carol Miller  | 2021-03-20  |
    And the following books exist:
      | isbn            | title                | author       | copies |
      | 9781234567890   | Sample Book          | John Doe     | 3      |
      | 9781111111111   | Data Structures      | John Doe     | 5      |
      | 9782222222222   | Machine Learning 101 | Carlos Lopez | 4      |

  @happy
  Scenario: Simple checkout returns OK and updates views
    When I checkout books for account "ACC123456":
      | bookId          |
      | 9781234567890.1 |
      | 9781111111111.3 |
    Then the activity operation notes should be:
      | bookId          | note |
      | 9781234567890.1 | Ok   |
      | 9781111111111.3 | Ok   |
    And catalog availability should be:
      | isbn            | available |
      | 9781234567890   | 2         |
      | 9781111111111   | 4         |
    And user "ACC123456" should have borrowed books:
      | bookId          |
      | 9781234567890.1 |
      | 9781111111111.3 |

  @mixed
  Scenario: Mixed results (unregistered + replacement in same request)
    Given the following book ids exist
      | bookId          | isbn            | accountNumber | title       | dueDate     | checkOutDate     |
      | 9781234567890.1 | 9781234567890   | ACC789012     | Sample Book | 2020-03-15  | 2020-02-15  |
    When I checkout books for account "ACC123456":
      | bookId                |
      | 9781234567890.1       |
      | 9999999999999.ghost   |
    Then the activity operation notes should be:
      | bookId                | note                                                   |
      | 9781234567890.1       | Book was already checked out, replaced with new record |
      | 9999999999999.ghost   | Book is not registered in the catalog                  |
    And catalog availability should be:
      | isbn                  | available |
      | 9781234567890         | 2         |
    And user "ACC123456" should have borrowed books:
      | bookId                |
      | 9781234567890.1       |
