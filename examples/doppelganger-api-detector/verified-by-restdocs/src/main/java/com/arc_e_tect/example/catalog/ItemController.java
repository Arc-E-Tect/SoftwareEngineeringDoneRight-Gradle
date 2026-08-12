package com.arc_e_tect.example.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/items")
public class ItemController {

    @GetMapping("/{id}")
    public String getItem(@PathVariable Long id) {
        // Verified by a real Spring RestDocs test - see src/test/java.
        return "{}";
    }

    @GetMapping
    public String listItems() {
        // Declared in openapi.yaml and implemented here, but no test covers it - the doppelganger
        // API this example is designed to still catch, even though getItem above is genuinely
        // verified.
        return "[]";
    }
}
