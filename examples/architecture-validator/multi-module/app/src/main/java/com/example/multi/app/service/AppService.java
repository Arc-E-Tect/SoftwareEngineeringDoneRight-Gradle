package com.example.multi.app.service;

import com.example.multi.adapters.persistence.DatabaseAdapter;
import com.example.multi.domain.model.Order;

public class AppService {

    private final DatabaseAdapter databaseAdapter = new DatabaseAdapter();

    public void createOrder(String id) {
        databaseAdapter.save(new Order(id));
    }
}