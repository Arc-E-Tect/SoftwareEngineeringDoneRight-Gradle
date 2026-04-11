package com.arc_e_tect.gradle.jacoco.model;

import com.arc_e_tect.gradle.jacoco.model.ExcludedElement.ElementType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExcludedElement")
class ExcludedElementTest {

    // ── ElementType.label() ───────────────────────────────────────────────────

    @Test
    @DisplayName("CLASS label is 'Class'")
    void classLabel() {
        assertThat(ElementType.CLASS.label()).isEqualTo("Class");
    }

    @Test
    @DisplayName("CONSTRUCTOR label is 'Constructor'")
    void constructorLabel() {
        assertThat(ElementType.CONSTRUCTOR.label()).isEqualTo("Constructor");
    }

    @Test
    @DisplayName("METHOD label is 'Method'")
    void methodLabel() {
        assertThat(ElementType.METHOD.label()).isEqualTo("Method");
    }

    @Test
    @DisplayName("FIELD label is 'Field'")
    void fieldLabel() {
        assertThat(ElementType.FIELD.label()).isEqualTo("Field");
    }

    // ── Constructors ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("full constructor stores all fields")
    void fullConstructorStoresAllFields() {
        ExcludedElement e = new ExcludedElement(
                ElementType.METHOD, "com.example", "Foo", "doIt()", 42,
                "Foo.java", "some reason");

        assertThat(e.getType()).isEqualTo(ElementType.METHOD);
        assertThat(e.getPackageName()).isEqualTo("com.example");
        assertThat(e.getClassName()).isEqualTo("Foo");
        assertThat(e.getMember()).isEqualTo("doIt()");
        assertThat(e.getLineNumber()).isEqualTo(42);
        assertThat(e.getSourceFile()).isEqualTo("Foo.java");
        assertThat(e.getJustification()).isEqualTo("some reason");
    }

    @Test
    @DisplayName("short constructor defaults justification to empty string")
    void shortConstructorDefaultsJustification() {
        ExcludedElement e = new ExcludedElement(
                ElementType.FIELD, "com.example", "Bar", "x", 1, "Bar.java");

        assertThat(e.getJustification()).isEmpty();
    }

    @Test
    @DisplayName("null justification is coerced to empty string")
    void nullJustificationCoercedToEmpty() {
        ExcludedElement e = new ExcludedElement(
                ElementType.CLASS, "com.example", "Baz", "", 1, "Baz.java", null);

        assertThat(e.getJustification()).isEmpty();
    }

    // ── getFqcn() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getFqcn() returns package.class when package is not empty")
    void getFqcnWithPackage() {
        ExcludedElement e = element("com.example", "MyClass");
        assertThat(e.getFqcn()).isEqualTo("com.example.MyClass");
    }

    @Test
    @DisplayName("getFqcn() returns class name only when package is empty")
    void getFqcnWithoutPackage() {
        ExcludedElement e = element("", "MyClass");
        assertThat(e.getFqcn()).isEqualTo("MyClass");
    }

    // ── displayName() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("displayName() for CLASS shows class name")
    void displayNameClass() {
        ExcludedElement e = new ExcludedElement(ElementType.CLASS, "p", "Foo", "", 1, "Foo.java");
        assertThat(e.displayName()).isEqualTo("[class] Foo");
    }

    @Test
    @DisplayName("displayName() for CONSTRUCTOR shows member")
    void displayNameConstructor() {
        ExcludedElement e = new ExcludedElement(ElementType.CONSTRUCTOR, "p", "Foo", "Foo(int)", 1, "Foo.java");
        assertThat(e.displayName()).isEqualTo("[constructor] Foo(int)");
    }

    @Test
    @DisplayName("displayName() for METHOD shows member")
    void displayNameMethod() {
        ExcludedElement e = new ExcludedElement(ElementType.METHOD, "p", "Foo", "run()", 1, "Foo.java");
        assertThat(e.displayName()).isEqualTo("[method] run()");
    }

    @Test
    @DisplayName("displayName() for FIELD shows member")
    void displayNameField() {
        ExcludedElement e = new ExcludedElement(ElementType.FIELD, "p", "Foo", "myField", 1, "Foo.java");
        assertThat(e.displayName()).isEqualTo("[field] myField");
    }

    // ── toString() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("toString() contains type, fqcn, member, and line")
    void toStringContainsKeyInfo() {
        ExcludedElement e = new ExcludedElement(
                ElementType.METHOD, "com.example", "Foo", "run()", 7, "Foo.java");

        String s = e.toString();
        assertThat(s)
                .contains("METHOD")
                .contains("com.example.Foo")
                .contains("run()")
                .contains("7");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ExcludedElement element(String pkg, String cls) {
        return new ExcludedElement(ElementType.CLASS, pkg, cls, "", 1, "X.java");
    }
}
