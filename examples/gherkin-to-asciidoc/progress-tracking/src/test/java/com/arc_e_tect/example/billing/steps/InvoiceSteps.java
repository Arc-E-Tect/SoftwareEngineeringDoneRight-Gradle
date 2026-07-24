package com.arc_e_tect.example.billing.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for the "User pays an invoice" scenario in
 * {@code invoice.feature}, fully implemented to show that
 * {@code generateFeatureDocs} aggregates glue code found across every
 * directory in {@code glueCodeDirs}, not just one.
 */
public class InvoiceSteps {

    @Given("an outstanding invoice")
    public void anOutstandingInvoice() {
        // Not needed to demonstrate the gherkin-to-asciidoc plugin.
    }

    @When("the user pays the invoice")
    public void theUserPaysTheInvoice() {
        // Not needed to demonstrate the gherkin-to-asciidoc plugin.
    }

    @Then("the invoice is marked as paid")
    public void theInvoiceIsMarkedAsPaid() {
        // Not needed to demonstrate the gherkin-to-asciidoc plugin.
    }
}
