package com.arc_e_tect.example.auth.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for the "User logs in successfully" scenario in
 * {@code authentication.feature}.
 *
 * <p>Deliberately incomplete: the {@code Scenario Outline} steps
 * ({@code the user submits "..." and "..."} / {@code the result is "..."}) have no
 * matching step definition here, so {@code generateFeatureDocs} reports that
 * scenario as {@code defined} rather than {@code implemented}.</p>
 */
public class LoginSteps {

    @Given("the login page is open")
    public void theLoginPageIsOpen() {
        // Not needed to demonstrate the gherkin-to-asciidoc plugin.
    }

    @When("the user submits valid credentials")
    public void theUserSubmitsValidCredentials() {
        // Not needed to demonstrate the gherkin-to-asciidoc plugin.
    }

    @Then("the dashboard is displayed")
    public void theDashboardIsDisplayed() {
        // Not needed to demonstrate the gherkin-to-asciidoc plugin.
    }
}
