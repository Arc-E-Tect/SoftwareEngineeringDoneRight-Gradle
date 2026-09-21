package com.arc_e_tect.example.hexapp.domain;

// DELIBERATE VIOLATION §1 — Domain imports Adapter
// Rule triggered: "domain_must_only_depend_on_domain_or_jdk_core"
// The domain layer is the innermost ring. By importing an adapter class, it becomes coupled
// to persistence infrastructure. Changing the database technology would require changing
// domain code — a direct violation of the Dependency Inversion Principle. The domain model may
// only depend on itself and the configured JDK allow-list (java.lang, java.time, java.util,
// java.math by default) — never adapters, application services, ports, or third-party classes.
// See VIOLATIONS.md §1 for full explanation.
import com.arc_e_tect.example.hexapp.adapter.outbound.persistence.OrderJpaEntity;

/**
 * Domain model for an order. This class intentionally violates the Hexagonal Architecture
 * rule that the domain model may only depend on itself or the configured JDK allow-list.
 */
public class Order {

    private Long id;
    private String product;

    // DELIBERATE VIOLATION: a domain object must never reference a persistence entity directly.
    private OrderJpaEntity persistedForm;

    public Order(Long id, String product) {
        this.id = id;
        this.product = product;
    }

    public Long getId() {
        return id;
    }

    public String getProduct() {
        return product;
    }
}
