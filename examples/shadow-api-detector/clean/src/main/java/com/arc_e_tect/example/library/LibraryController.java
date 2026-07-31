package com.arc_e_tect.example.library;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/books")
public class LibraryController {

    @GetMapping
    public String listBooks() {
        return "[]";
    }

    @GetMapping("/{id}")
    public String getBook(@PathVariable Long id) {
        return "{}";
    }

    @PostMapping
    public String addBook(@RequestBody String book) {
        return book;
    }
}
