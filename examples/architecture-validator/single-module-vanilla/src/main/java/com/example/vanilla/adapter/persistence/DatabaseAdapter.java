package com.example.vanilla.adapter.persistence;

import com.example.vanilla.application.domain.model.Order;
import com.example.vanilla.application.port.out.OrderStore;

public class DatabaseAdapter implements OrderStore {

    @Override
    public void save(Order order) {
        // Example adapter implementation.
    }
}