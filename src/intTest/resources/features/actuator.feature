@service
Feature: Service actuator behavior

  Background:
    Given the service is running

  Scenario: Validate Health
    When I check the health endpoint
    Then the last response status should be 200

  Scenario: Validate info
    When I check the info endpoint
    Then the last response status should be 200