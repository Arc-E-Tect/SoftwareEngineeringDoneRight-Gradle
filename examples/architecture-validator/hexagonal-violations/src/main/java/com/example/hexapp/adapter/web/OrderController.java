package com.example.hexapp.adapter.web;

// DELIBERATE VIOLATION §2 — Adapter bypasses port, calls domain directly
// Rule triggered: "Adapters must not access domain classes directly — only via ports"
// The web adapter must communicate with the application through the inbound ports (use cases).
// By importing the domain model directly, the adapter couples itself to the domain layer and
// bypasses the port boundary — any change to the domain object forces a change in the adapter.
// See VIOLATIONS.md §2 for full explanation.
import com.example.hexapp.domain.Order;

/**
 * Web adapter (REST controller) for orders. This class intentionally violates the Hexagonal
 * Architecture rule that adapters must not access domain classes directly.
 */
public class OrderController {

    // DELIBERATE VIOLATION: the controller should depend on a port interface, not the domain model.
    public Order getOrder(Long id) {
        return new Order(id, "Widget");
    }
}
