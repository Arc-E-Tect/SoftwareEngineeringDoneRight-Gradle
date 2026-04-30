Feature: User Authentication

  Scenario: User logs in successfully
    Given a registered user
    When they enter valid credentials
    Then they should be logged in

  Scenario Outline: User logs in with different credentials
    Given a user with "<username>"
    When they enter their password
    Then they see "<result>"

    Examples:
      | username | result  |
      | admin    | success |
      | guest    | failure |
