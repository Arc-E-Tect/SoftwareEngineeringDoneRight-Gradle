package com.arc_e_tect.example.vanilla.application.service;

import com.arc_e_tect.example.vanilla.adapter.persistence.DatabaseAdapter;
import com.arc_e_tect.example.vanilla.application.domain.model.Order;

public class OrderApplicationService {

    private final DatabaseAdapter databaseAdapter = new DatabaseAdapter();

    public void createOrder(String id) {
        databaseAdapter.save(new Order(id));
    }
}