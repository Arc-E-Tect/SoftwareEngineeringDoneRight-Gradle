package com.example.fixture;

import com.arc_e_tect.book.sedr.jacoco.marker.ExcludeFromJacocoGeneratedCodeCoverage;

/**
 * Fixture where the justification is a constant reference (not a string literal).
 * Used to exercise the non-string-literal branch in AnnotationScanner#extractJustification.
 */
public class ConstantJustificationFixture {

    static final String REASON = "Some reason";

    @ExcludeFromJacocoGeneratedCodeCoverage(justification = ConstantJustificationFixture.REASON)
    public void method() {}
}
