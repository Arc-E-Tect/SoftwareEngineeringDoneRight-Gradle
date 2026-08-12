package com.arc_e_tect.example.catalog;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/items")
public class ItemController {

    @GetMapping
    public String listItems() {
        // Verified by a real Spring RestDocs test - see src/test/java.
        return "[]";
    }

    @GetMapping("/{id}")
    public String getItem(@PathVariable Long id) {
        // Verified by a real REST Assured test with live OpenAPI validation - see src/test/java.
        return "{}";
    }

    @PutMapping("/{id}")
    public String updateItem(@PathVariable Long id, @RequestBody String item) {
        // Verified by a real Spring Cloud Contract file whose concrete example URL is /items/1 -
        // see src/test/resources/contracts.
        return item;
    }

    @DeleteMapping("/{id}")
    public void deleteItem(@PathVariable Long id) {
        // Declared in openapi.yaml and implemented here, but nothing verifies it - the
        // doppelganger API this example is designed to still catch, even with all three
        // verification sources enabled and working for its siblings above.
    }
}
