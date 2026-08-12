package com.arc_e_tect.example.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/items")
public class ItemController {

    @GetMapping
    public String listItems() {
        return "[]";
    }

    @PostMapping
    public String createItem(@RequestBody String item) {
        return item;
    }

    // Genuinely implemented - a real, working handler exists. It's still reported as a mirage
    // API by this example: scanMocks = true means controllers like this one are never consulted,
    // only WireMock stubs are - and no stub exists for this endpoint (see
    // src/test/resources/mappings). This is the point of the example: "implemented" is redefined
    // entirely, not just checked in addition to controller scanning.
    @GetMapping("/{id}")
    public String getItem(@PathVariable Long id) {
        return "{}";
    }
}
