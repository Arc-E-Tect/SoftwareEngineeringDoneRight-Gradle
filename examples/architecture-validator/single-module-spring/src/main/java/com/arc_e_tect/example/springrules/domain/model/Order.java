package com.arc_e_tect.example.springrules.domain.model;

public class Order {

    private final String customerId;

    public Order(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerId() {
        return customerId;
    }
}
