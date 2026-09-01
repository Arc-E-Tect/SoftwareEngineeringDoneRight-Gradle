Feature: Checkout

  Scenario: User logs in successfully
    Given a returning customer at checkout
    When they sign in with their saved account
    Then their saved shipping address is pre-filled
