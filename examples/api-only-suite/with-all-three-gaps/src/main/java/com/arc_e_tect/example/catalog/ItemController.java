package com.arc_e_tect.example.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/items")
public class ItemController {

    @GetMapping
    public String listItems() {
        // Declared in openapi.yaml AND implemented here, but this module has no src/test/java and
        // no src/test/resources/contracts - no verification evidence exists for it. The
        // doppelganger API this example is designed to catch.
        return "[]";
    }

    @GetMapping("/{id}")
    public String getItem(@PathVariable Long id) {
        // Implemented here, but not described in openapi.yaml at all. The shadow API this
        // example is designed to catch.
        return "{}";
    }

    // Note: openapi.yaml also declares POST /items (createItem), which has no handler here at
    // all. The mirage API this example is designed to catch.
}
