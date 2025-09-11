@account
Feature: Account API behavior

  Background:
    Given the service is running
    And the following accounts exist:
      | accountNumber | name          | memberSince |
      | ACC123456     | Alice Smith   | 2020-01-15  |
      | ACC789012     | Bob Johnson   | 2022-09-10  |
      | ACC345678     | Carol Miller  | 2021-03-20  |

  Scenario Outline: Get account by number
    When I request account "<user>"
    Then the response should be for account "<user>"
    And the last response status should be 200
    Examples:
      | user      |
      | ACC123456 |
      | ACC789012 |
      | ACC345678 |

  Scenario: Get account by number that doesn’t exist
    When I request account "99999"
    And the last response status should be 404

  Scenario Outline: Request account with invalid number
    When I request account with invalid number "<user>"
    Then the last response status should be 400
    Examples:
      | user                |
      | abc!def             |
      | bc@def.com          |
      |  account#001        |
      |  name/with/slas#001 |