package com.arc_e_tect.example.library;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/library")
public class LibraryController {

    @RequestMapping(path = "/books", method = RequestMethod.GET)
    public String listBooks() {
        return "[]";
    }

    @RequestMapping(path = "/books/{id}", method = RequestMethod.GET)
    public String getBook(@PathVariable Long id) {
        return "{}";
    }

    @RequestMapping(path = "/books", method = RequestMethod.POST)
    public String addBook(@RequestBody String book) {
        return book;
    }
}
