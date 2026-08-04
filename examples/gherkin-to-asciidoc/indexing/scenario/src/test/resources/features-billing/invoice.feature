Feature: Invoice payment

  Scenario: 4 - User pays an invoice
    Given an outstanding invoice
    When the user pays the invoice
    Then the invoice is marked as paid
