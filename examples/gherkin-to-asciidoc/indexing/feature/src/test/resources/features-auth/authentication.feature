Feature: 1 - User authentication

  Scenario: User requests a password reset
    # Not yet fleshed out - title only, no steps yet.

  Scenario: User logs in successfully
    Given the login page is open
    When the user submits valid credentials
    Then the dashboard is displayed

  Scenario Outline: User logs in with different credential sets
    Given the login page is open
    When the user submits "<username>" and "<password>"
    Then the result is "<outcome>"

    Examples:
      | username | password | outcome |
      | alice    | secret   | success |
      | bob      | wrong    | failure |
