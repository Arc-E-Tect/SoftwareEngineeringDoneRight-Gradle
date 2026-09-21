package com.arc_e_tect.example.hexapp.adapter.inbound.web;

// DELIBERATE VIOLATION §2 — Inbound adapter depends on a service implementation directly.
// Rules triggered: "adapters_must_not_depend_on_domain_services_directly" and
// "only_configuration_may_depend_on_domain_service_implementations"
// The inbound path should stop at a use-case interface. By binding the controller directly to
// the concrete service class, the example bypasses the inbound port boundary.
import com.arc_e_tect.example.hexapp.application.OrderService;
import com.arc_e_tect.example.hexapp.domain.Order;

/**
 * Web adapter (REST controller) for orders.
 */
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public Order getOrder(Long id) {
        return orderService.getOrder(id);
    }
}