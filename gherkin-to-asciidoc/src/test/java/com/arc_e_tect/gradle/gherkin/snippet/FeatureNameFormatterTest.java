package com.arc_e_tect.gradle.gherkin.snippet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FeatureNameFormatter")
class FeatureNameFormatterTest {

    @Test
    @DisplayName("converts a multi-word title to camelCase")
    void convertsMultiWordTitleToCamelCase() {
        assertThat(FeatureNameFormatter.toDirectoryName("User authentication")).isEqualTo("userAuthentication");
    }

    @Test
    @DisplayName("lower-cases a single-word title")
    void lowerCasesSingleWordTitle() {
        assertThat(FeatureNameFormatter.toDirectoryName("Billing")).isEqualTo("billing");
    }

    @Test
    @DisplayName("collapses multiple consecutive spaces")
    void collapsesMultipleConsecutiveSpaces() {
        assertThat(FeatureNameFormatter.toDirectoryName("Invoice   payment   flow")).isEqualTo("invoicePaymentFlow");
    }

    @Test
    @DisplayName("trims leading and trailing whitespace")
    void trimsLeadingAndTrailingWhitespace() {
        assertThat(FeatureNameFormatter.toDirectoryName("  User authentication  ")).isEqualTo("userAuthentication");
    }

    @Test
    @DisplayName("returns an empty string for a blank title")
    void returnsEmptyStringForBlankTitle() {
        assertThat(FeatureNameFormatter.toDirectoryName("   ")).isEmpty();
    }
}
