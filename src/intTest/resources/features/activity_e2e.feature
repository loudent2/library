@flow
Feature: End-to-end activity flow updates user and catalog consistently

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

  Scenario: Checkout -> validate -> Checkin -> validate
    When I checkout books for account "ACC123456":
      | bookId          |
      | 9782222222222.1 |
      | 9781234567890.2 |
    Then the activity operation notes should be:
      | bookId          | note |
      | 9782222222222.1 | Ok   |
      | 9781234567890.2 | Ok   |
    And catalog availability should be:
      | isbn          | available |
      | 9782222222222 | 3         |
      | 9781234567890 | 2         |
    And user "ACC123456" should have borrowed books:
      | bookId          |
      | 9781234567890.2 |
      | 9782222222222.1 |

    When I checkin books:
      | bookId          |
      | 9782222222222.1 |
      | 9781234567890.2 |
    Then the activity operation notes should be:
      | bookId          | note |
      | 9782222222222.1 | Ok   |
      | 9781234567890.2 | Ok   |
    And catalog availability should be:
      | isbn          | available |
      | 9782222222222 | 4         |
      | 9781234567890 | 3         |
    And user "ACC123456" should have no borrowed books:
