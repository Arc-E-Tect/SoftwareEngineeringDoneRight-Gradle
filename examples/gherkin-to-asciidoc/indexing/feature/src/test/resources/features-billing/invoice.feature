Feature: 2 - Invoice payment

  Scenario: User pays an invoice
    Given an outstanding invoice
    When the user pays the invoice
    Then the invoice is marked as paid
