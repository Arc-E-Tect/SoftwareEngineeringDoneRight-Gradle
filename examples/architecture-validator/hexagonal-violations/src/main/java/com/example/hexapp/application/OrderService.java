package com.example.hexapp.application;

// DELIBERATE VIOLATION §3 — Application service imports Adapter
// Rule triggered: "Application services must not depend on adapters"
// The application layer should depend on outbound port interfaces, not concrete adapter
// implementations. By importing JpaOrderAdapter directly, the service is coupled to the
// persistence technology — swapping to a different database would require changes here.
// See VIOLATIONS.md §3 for full explanation.
import com.example.hexapp.adapter.persistence.JpaOrderAdapter;

/**
 * Application service for order processing. This class intentionally violates the Hexagonal
 * Architecture rule that application services must not depend on adapters.
 */
public class OrderService {

    // DELIBERATE VIOLATION: should depend on an outbound port interface, not the concrete adapter.
    private final JpaOrderAdapter adapter;

    public OrderService(JpaOrderAdapter adapter) {
        this.adapter = adapter;
    }
}
