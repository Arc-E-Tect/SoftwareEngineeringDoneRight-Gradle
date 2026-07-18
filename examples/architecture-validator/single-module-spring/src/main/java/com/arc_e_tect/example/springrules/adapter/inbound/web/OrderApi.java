package com.arc_e_tect.example.springrules.adapter.inbound.web;

import com.arc_e_tect.example.springrules.application.port.inbound.CreateOrderUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Intentionally named without a {@code Controller} suffix to demonstrate the opt-in
 * {@code namingConventionsEnabled} naming rules from the companion Spring rule pack.
 */
@RestController
public class OrderApi {

    private final CreateOrderUseCase createOrderUseCase;

    public OrderApi(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }

    @PostMapping("/orders")
    public void createOrder(@RequestParam String customerId) {
        createOrderUseCase.createOrder(customerId);
    }
}
