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
        // Verified by a real, running-server Spring RestDocs test - see src/test/java. Note the
        // mapping has no "catalog-service" prefix: that's the servlet context path (see
        // application.yml and openapi.yaml's servers[0].url), added by Spring itself, not part of
        // this controller's own mapping.
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
