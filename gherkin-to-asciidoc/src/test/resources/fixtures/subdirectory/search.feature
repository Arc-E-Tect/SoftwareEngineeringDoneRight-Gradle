Feature: Product Search

  Scenario: User searches for products by keyword
    Given the search page is open
    When the user enters "laptop"
    Then results are displayed on screen
