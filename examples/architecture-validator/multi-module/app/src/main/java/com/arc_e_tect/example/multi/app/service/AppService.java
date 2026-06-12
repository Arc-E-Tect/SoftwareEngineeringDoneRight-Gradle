package com.arc_e_tect.example.multi.app.service;

import com.arc_e_tect.example.multi.adapters.persistence.DatabaseAdapter;
import com.arc_e_tect.example.multi.domain.model.Order;

public class AppService {

    private final DatabaseAdapter databaseAdapter = new DatabaseAdapter();

    public void createOrder(String id) {
        databaseAdapter.save(new Order(id));
    }
}