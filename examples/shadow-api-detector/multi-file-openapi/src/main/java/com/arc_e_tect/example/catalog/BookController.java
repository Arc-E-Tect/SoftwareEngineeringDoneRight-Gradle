package com.arc_e_tect.example.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/books")
public class BookController {

    @GetMapping
    public String listBooks() {
        return "[]";
    }

    @GetMapping("/{id}")
    public String getBook(@PathVariable Long id) {
        return "{}";
    }
}
