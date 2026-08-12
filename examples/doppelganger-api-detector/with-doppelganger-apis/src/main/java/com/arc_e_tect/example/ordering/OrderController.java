package com.arc_e_tect.example.ordering;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @GetMapping
    public String listOrders() {
        return "[]";
    }

    @GetMapping("/{id}")
    public String getOrder(@PathVariable Long id) {
        return "{}";
    }

    @PostMapping
    public String createOrder(@RequestBody String order) {
        return order;
    }

    // Note: every operation below is both declared in openapi.yaml and implemented here, but this
    // module has no src/test/java and no src/test/resources/contracts - none of the three
    // contract verification sources (Spring RestDocs, the OpenAPI request validator, Spring Cloud
    // Contract) has any evidence for any of them. All three are the doppelganger APIs this
    // example is designed to catch.
}
