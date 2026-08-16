package com.arc_e_tect.example.microservices.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/items")
public class ItemController {

    @GetMapping("/{id}")
    public String getItem(@PathVariable Long id) {
        // Declared in openapi.yaml, implemented here, and verified by
        // ItemControllerValidatorTest - compliant.
        return "{}";
    }

    @GetMapping
    public String listItems() {
        // Declared in openapi.yaml and implemented here, but no test covers it - the
        // doppelganger API this service is designed to catch.
        return "[]";
    }

    @GetMapping("/{id}/reviews")
    public String getItemReviews(@PathVariable Long id) {
        // Implemented here, but never described in openapi.yaml - the shadow API this
        // service is designed to catch.
        return "[]";
    }
}
