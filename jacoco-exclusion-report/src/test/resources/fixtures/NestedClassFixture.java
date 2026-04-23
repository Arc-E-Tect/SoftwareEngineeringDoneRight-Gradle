package com.example.fixture;

import com.arc_e_tect.book.sedr.jacoco.marker.ExcludeFromJacocoGeneratedCodeCoverage;

/**
 * Fixture with a nested (inner static) class that carries the exclusion annotation.
 * Used to exercise the recursive buildClassName() path in AnnotationScanner.
 */
public class NestedClassFixture {

    public static class Inner {

        @ExcludeFromJacocoGeneratedCodeCoverage
        public void innerMethod() {}
    }
}
