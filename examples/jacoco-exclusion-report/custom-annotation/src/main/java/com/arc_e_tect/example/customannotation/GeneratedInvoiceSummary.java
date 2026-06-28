package com.arc_e_tect.example.customannotation;

import java.util.List;

@GeneratedExclusion
public class GeneratedInvoiceSummary {

    private final String invoiceId;
    private final List<String> lineItems;

    @GeneratedExclusion
    public GeneratedInvoiceSummary(String invoiceId, List<String> lineItems) {
        this.invoiceId = invoiceId;
        this.lineItems = List.copyOf(lineItems);
    }

    @GeneratedExclusion
    public String render() {
        return "Invoice " + invoiceId + " -> " + lineItems;
    }
}