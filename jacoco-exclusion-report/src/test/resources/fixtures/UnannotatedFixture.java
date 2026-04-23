package com.example.fixture;

/**
 * Fixture WITHOUT the exclusion annotation.
 * AnnotationScannerTest uses this to verify no false positives are reported.
 */
public class UnannotatedFixture {

    private String value;

    public UnannotatedFixture(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void doSomething(int times) {
        for (int i = 0; i < times; i++) {
            System.out.println(value);
        }
    }
}
