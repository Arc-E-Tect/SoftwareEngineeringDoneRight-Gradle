package com.arc_e_tect.example.springrules.adapter.outbound.persistence;

import com.arc_e_tect.example.springrules.application.port.outbound.OrderStorePort;
import com.arc_e_tect.example.springrules.domain.model.Order;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class OrderStoreAdapter implements OrderStorePort {

    private final List<Order> orders = new ArrayList<>();

    @Override
    public void save(Order order) {
        orders.add(order);
    }
}
