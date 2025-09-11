@catalog
Feature: Catalog availability behavior

  Background:
    Given the service is running
    And the following books exist:
      | isbn            | title                | author       | copies |
      | 9781234567890   | Sample Book          | John Doe     | 3      |
      | 9781111111111   | Data Structures      | John Doe     | 5      |
      | 9782222222222   | Machine Learning 101 | Carlos Lopez | 4      |

  Scenario: Validate Health
    When I check the health endpoint
    Then the last response status should be 200

  Scenario Outline: Get book by title
    When I request catalog by title "<title>"
    Then the response should be for title "<title>"
    Then the last response status should be 200
    Examples:
      | title                |
      | Sample Book          |
      | Data Structures      |
      | Machine Learning 101 |

  Scenario Outline: Get book by isbn
    When I request catalog by isbn "<isbn>"
    Then the response should be for isbn "<isbn>"
    Then the last response status should be 200
    Examples:
      | isbn          |
      | 9781234567890 |
      | 9781111111111 |
      | 9782222222222 |

  Scenario Outline: Get book by title no book exists
    When I request catalog by title "<title>"
    Then Response Should include message for book title "<title>"
    Then the last response status should be 404
    Examples:
      | title     |
      | Bad Book  |

  Scenario Outline: Get book by isbn when no book exists
    When I request catalog by isbn "<isbn>"
    Then Response Should include message for book isbn "<isbn>"
    Then the last response status should be 404
    Examples:
      | isbn          |
      | 9999999999999 |

  Scenario Outline: Successfully search catalog by author Last name
    When I search Catalog by author last name "<lastname>"
    Then the response should include <numbooks> books for author last name "<lastname>"
    And the last response status should be 200
    Examples:
    | lastname | numbooks |
    | Doe      | 2        |
    | Lopez    | 1        |

  Scenario Outline: Unsuccessfully search catalog by author Last name
    When I search Catalog by author last name "<lastname>"
    Then the response should have no books
    And the last response status should be 200
    Examples:
      | lastname | numbooks |
      | Fett     | 0        |

  Scenario Outline: Get book by invalid isbn should return 400
    When I request catalog by isbn "<isbn>"
    Then the last response status should be 400
    Examples:
      | isbn          |
      | 123           |
      | abcde1234567  |
      | 123456789012  |
      | 12345678901234|

  Scenario Outline: Get book by title with invalid request body should return 400
    When I request catalog item with invalid title "<title>"
    Then the last response status should be 400
    Examples:
      | title   |
      |         |
      | null    |


  Scenario Outline: Search catalog with invalid search request should return 400
    When I search Catalog by bad author last name "<lastname>"
    Then the last response status should be 400
    Examples:
      | lastname |
      |          |
