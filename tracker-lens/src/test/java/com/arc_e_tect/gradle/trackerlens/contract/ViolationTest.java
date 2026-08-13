package com.arc_e_tect.gradle.trackerlens.contract;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Violation")
class ViolationTest {

    @Test
    @DisplayName("toStringShouldCombineRuleNameAndMessage")
    void toStringShouldCombineRuleNameAndMessage() {
        Violation violation = new Violation(ContractRule.DASHBOARD_ROOT, "found 2");

        assertThat(violation.toString()).isEqualTo("DASHBOARD_ROOT: found 2");
    }
}
