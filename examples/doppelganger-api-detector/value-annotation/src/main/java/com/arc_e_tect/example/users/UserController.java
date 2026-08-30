package com.arc_e_tect.example.users;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
public class UserController {

    @GetMapping("/{username}")
    public String getUserByUsername(@PathVariable String username) {
        // Verified by a real Spring RestDocs test that resolves its request path via a field
        // annotated @Value("${users.by-username}"), injected by Spring from
        // application.properties - not a string literal - see src/test/java.
        return "{}";
    }

    @GetMapping
    public String listUsers() {
        // Declared in openapi.yaml and implemented here, but no test covers it - the doppelganger
        // API this example is designed to still catch, even though getUserByUsername above is
        // genuinely verified.
        return "[]";
    }
}
