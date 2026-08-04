Feature: User authentication

  Scenario: 1 - User requests a password reset
    # Not yet fleshed out - title only, no steps yet.

  Scenario: 2 - User logs in successfully
    Given the login page is open
    When the user submits valid credentials
    Then the dashboard is displayed

  Scenario Outline: 3 - User logs in with different credential sets
    Given the login page is open
    When the user submits "<username>" and "<password>"
    Then the result is "<outcome>"

    Examples:
      | username | password | outcome |
      | alice    | secret   | success |
      | bob      | wrong    | failure |
