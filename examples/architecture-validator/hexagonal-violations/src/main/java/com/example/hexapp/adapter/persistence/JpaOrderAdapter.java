package com.example.hexapp.adapter.persistence;

/**
 * JPA adapter for persisting orders. Lives correctly in the adapters layer.
 *
 * <p>Referenced (incorrectly) by {@link com.example.hexapp.application.OrderService} — see
 * VIOLATIONS.md §3.</p>
 */
public class JpaOrderAdapter {

    public void save(OrderJpaEntity entity) {
        // persistence implementation
    }
}
