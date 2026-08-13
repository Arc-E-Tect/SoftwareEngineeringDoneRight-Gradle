package com.arc_e_tect.gradle.trackerlens.contract;

/**
 * One {@link ContractRule} violation found by {@link LensContractValidator}.
 *
 * @param rule    the violated rule
 * @param message a human-readable description of what was found instead
 */
public record Violation(ContractRule rule, String message) {

    @Override
    public String toString() {
        return rule.name() + ": " + message;
    }
}
