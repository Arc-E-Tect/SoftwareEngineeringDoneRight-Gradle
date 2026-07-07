package com.arc_e_tect.example.scopesplit.application.service;

import com.arc_e_tect.example.scopesplit.application.domain.model.Order;
import com.arc_e_tect.example.scopesplit.application.port.outbound.OrderOutputPort;

public class OrderApplicationService {

    private final OrderOutputPort orderOutputPort;

    public OrderApplicationService(OrderOutputPort orderOutputPort) {
        this.orderOutputPort = orderOutputPort;
    }

    public void createOrder(String id) {
        Order order = new Order(id);
        orderOutputPort.persistOrder(order.id());
    }
}
