package com.arc_e_tect.example.hybrid;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class CustomImperativeTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.arc_e_tect.example.hybrid");

    @Test
    void applicationLayerShouldNotDependOnAdaptersWhenUsingImperativeStyle() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..adapter..")
                .because("Imperative ArchUnit tests can coexist with generated and annotation-based tests")
                .check(classes);
    }
}