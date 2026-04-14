package com.example.fixture;

import com.arc_e_tect.book.sedr.jacoco.marker.ExcludeFromJacocoGeneratedCodeCoverage;

/**
 * Fixture class used by AnnotationScannerTest.
 *
 * Annotations are placed on:
 *  – the class itself (line ~10)                      with justification
 *  – a field           (line ~15)                      without justification
 *  – a constructor     (line ~19)                      with justification
 *  – a method          (line ~26)                      with justification
 */
@ExcludeFromJacocoGeneratedCodeCoverage(justification = "Framework-managed class, not unit-testable")
public class AnnotatedFixture {

    @ExcludeFromJacocoGeneratedCodeCoverage
    private String excludedField;

    @ExcludeFromJacocoGeneratedCodeCoverage(justification = "Boilerplate constructor")
    public AnnotatedFixture(String value) {
        this.excludedField = value;
    }

    public AnnotatedFixture() {
        this.excludedField = "";
    }

    @ExcludeFromJacocoGeneratedCodeCoverage(justification = "Delegation method only")
    public String excludedMethod(int count, String label) {
        return label.repeat(count);
    }

    public String notExcluded() {
        return "no annotation here";
    }
}
