Feature: Checkout

  Scenario: Shopper completes checkout
    Given the shopping cart has items
    When the shopper submits payment details
    Then the order confirmation is displayed
