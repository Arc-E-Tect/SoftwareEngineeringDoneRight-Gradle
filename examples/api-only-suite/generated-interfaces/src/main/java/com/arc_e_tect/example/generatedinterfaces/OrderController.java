package com.arc_e_tect.example.generatedinterfaces;

import com.arc_e_tect.example.generatedinterfaces.api.OrderV1;
import com.arc_e_tect.example.generatedinterfaces.api.OrdersApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Implements the interface generated from the contract. It declares no path, verb or parameter
 * mapping of its own: every one is inherited from {@link OrdersApi}, in another file, generated
 * under build/generated/openapi on every build.
 */
@RestController
public class OrderController implements OrdersApi {

    @Override
    public ResponseEntity<List<OrderV1>> listOrders() {
        return ResponseEntity.ok(List.of(order(1L)));
    }

    @Override
    public ResponseEntity<OrderV1> getOrder(Long id) {
        return ResponseEntity.ok(order(id));
    }

    private static OrderV1 order(Long id) {
        return new OrderV1().id(id).status(OrderV1.StatusEnum.OPEN);
    }
}
