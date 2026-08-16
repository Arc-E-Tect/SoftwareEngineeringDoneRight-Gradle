package com.arc_e_tect.example.microservices.users;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/{id}")
    public String getUser(@PathVariable Long id) {
        // Declared in openapi.yaml, implemented here, and verified by
        // UserControllerValidatorTest - compliant.
        return "{}";
    }

    @GetMapping
    public String listUsers() {
        // Declared in openapi.yaml and implemented here, but no test covers it - the
        // doppelganger API this service is designed to catch.
        return "[]";
    }

    @GetMapping("/{id}/preferences")
    public String getUserPreferences(@PathVariable Long id) {
        // Implemented here, but never described in openapi.yaml - the shadow API this
        // service is designed to catch.
        return "{}";
    }
}
