package com.arc_e_tect.example.invoicing;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/invoices")
public class InvoiceController {

    @GetMapping
    public String listInvoices() {
        return "[]";
    }

    @GetMapping("/{id}")
    public String getInvoice(@PathVariable Long id) {
        return "{}";
    }

    @PostMapping
    public String createInvoice(@RequestBody String invoice) {
        return invoice;
    }

    // Added after the OpenAPI document was last updated - never described. This is the
    // shadow API this example is designed to catch.
    @DeleteMapping("/{id}")
    public void deleteInvoice(@PathVariable Long id) {
    }
}
