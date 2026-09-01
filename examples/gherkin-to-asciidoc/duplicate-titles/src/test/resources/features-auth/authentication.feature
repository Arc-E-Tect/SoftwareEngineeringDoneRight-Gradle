Feature: User authentication

  Scenario: User logs in successfully
    Given the login page is open
    When the user submits valid credentials
    Then the dashboard is displayed
