package com.example.libraryrules.application.service;

import com.example.libraryrules.adapter.persistence.DatabaseAdapter;
import com.example.libraryrules.application.domain.model.Order;

public class OrderApplicationService {

    private final DatabaseAdapter databaseAdapter = new DatabaseAdapter();

    public void createOrder(String id) {
        databaseAdapter.save(new Order(id));
    }
}