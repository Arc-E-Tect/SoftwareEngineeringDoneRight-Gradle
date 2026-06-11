package com.example.hexapp;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * User-provided ArchUnit test — runs in the same {@code testArchitecture} suite as the
 * plugin-generated {@code GeneratedHexagonalArchTest}.
 *
 * <p>No base class, interface, or naming convention imposed by the plugin is required here.
 * This class demonstrates that hand-written tests and generated tests coexist freely in
 * {@code src/testArchitecture/java}.</p>
 */
@AnalyzeClasses(packages = "com.example.hexapp")
public class NamingConventionTest {

    @ArchTest
    static final ArchRule controllers_should_be_named_correctly =
        classes()
            .that().resideInAPackage("..adapter.web..")
            .should().haveSimpleNameEndingWith("Controller")
            .allowEmptyShould(true)
            .as("Web adapter classes must end with 'Controller'");
}
