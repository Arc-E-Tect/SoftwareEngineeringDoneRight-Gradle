package com.arc_e_tect.example.catalog;

import org.springframework.web.bind.annotation.GetMapping;
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
}
