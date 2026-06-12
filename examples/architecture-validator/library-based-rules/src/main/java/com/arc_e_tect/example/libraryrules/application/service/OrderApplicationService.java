package com.arc_e_tect.example.libraryrules.application.service;

import com.arc_e_tect.example.libraryrules.adapter.persistence.DatabaseAdapter;
import com.arc_e_tect.example.libraryrules.application.domain.model.Order;

public class OrderApplicationService {

    private final DatabaseAdapter databaseAdapter = new DatabaseAdapter();

    public void createOrder(String id) {
        databaseAdapter.save(new Order(id));
    }
}