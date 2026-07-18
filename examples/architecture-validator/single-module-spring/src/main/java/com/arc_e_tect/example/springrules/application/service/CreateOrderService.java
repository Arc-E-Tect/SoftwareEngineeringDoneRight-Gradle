package com.arc_e_tect.example.springrules.application.service;

import com.arc_e_tect.example.springrules.application.port.inbound.CreateOrderUseCase;
import com.arc_e_tect.example.springrules.application.port.outbound.OrderStorePort;
import com.arc_e_tect.example.springrules.domain.model.Order;
import org.springframework.stereotype.Service;

/**
 * Intentionally annotated with {@code @Service} to demonstrate {@code rulesDisabled}: the
 * companion Spring rule pack's {@code DomainIsolationTest.applicationServicesShouldNotCarrySpringStereotypes}
 * would otherwise fail here, but the build disables that specific rule.
 */
@Service
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderStorePort orderStorePort;

    public CreateOrderService(OrderStorePort orderStorePort) {
        this.orderStorePort = orderStorePort;
    }

    @Override
    public void createOrder(String customerId) {
        orderStorePort.save(new Order(customerId));
    }
}
