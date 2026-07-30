Feature: Product catalog

  Scenario: Shopper browses the catalog
    Given the catalog page is open
    When the shopper searches for "espresso machine"
    Then matching products are displayed
