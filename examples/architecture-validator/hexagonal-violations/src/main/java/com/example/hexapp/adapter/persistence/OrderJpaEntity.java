package com.example.hexapp.adapter.persistence;

/**
 * JPA entity for persisting orders. Lives correctly in the adapters layer.
 *
 * <p>Referenced (incorrectly) by {@link com.example.hexapp.domain.Order} — see
 * VIOLATIONS.md §1.</p>
 */
public class OrderJpaEntity {

    private Long id;
    private String product;

    public OrderJpaEntity(Long id, String product) {
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
