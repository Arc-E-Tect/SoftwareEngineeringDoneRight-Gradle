package com.example.hybrid.application.service;

import com.example.hybrid.adapter.persistence.DatabaseAdapter;
import com.example.hybrid.application.domain.model.Order;

public class OrderApplicationService {

    private final DatabaseAdapter databaseAdapter = new DatabaseAdapter();

    public void createOrder(String id) {
        databaseAdapter.save(new Order(id));
    }
}