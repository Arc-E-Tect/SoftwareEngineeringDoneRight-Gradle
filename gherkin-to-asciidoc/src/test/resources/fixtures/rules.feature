Feature: Rule-Based Scenarios

  Rule: Registered users can access premium content

    Scenario: Premium user views protected page
      Given a user with a premium subscription
      When they navigate to a protected page
      Then the page content is displayed

    Scenario Outline: Premium user accesses different content types
      Given a premium user
      When they access "<content_type>"
      Then they can view the content

      Examples:
        | content_type |
        | articles     |
        | videos       |
