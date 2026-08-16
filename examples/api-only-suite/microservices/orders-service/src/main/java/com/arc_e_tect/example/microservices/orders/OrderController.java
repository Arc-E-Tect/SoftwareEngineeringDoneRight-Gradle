package com.arc_e_tect.example.microservices.orders;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @GetMapping("/{id}")
    public String getOrder(@PathVariable Long id) {
        // Declared in openapi.yaml, implemented here, and verified by
        // OrderControllerValidatorTest - compliant.
        return "{}";
    }

    @PostMapping
    public String createOrder(@RequestBody String order) {
        // Declared in openapi.yaml and implemented here, but no test covers it - the
        // doppelganger API this service is designed to catch.
        return order;
    }

    @DeleteMapping("/{id}")
    public void cancelOrder(@PathVariable Long id) {
        // Implemented here, but never described in openapi.yaml - the shadow API this
        // service is designed to catch.
    }
}
