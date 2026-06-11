package com.arc_e_tect.gradle.architecture.spring;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class DependencyDirectionTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RulePackConfiguration.basePackage());

    @Test
    void coreApplicationLayerShouldNotDependOnAdapters() {
        noClasses()
                .that().resideOutsideOfPackage("..adapter..")
                .and().resideOutsideOfPackage("..adapters..")
                .should().dependOnClassesThat().resideInAnyPackage(RulePackConfiguration.adapters())
                .because("Adapters are outer-layer details; the application core must stay independent")
                .check(classes);
    }

    @Test
    void adaptersShouldNotDependOnServiceImplementations() {
        noClasses()
                .that().resideInAnyPackage(RulePackConfiguration.adapters())
                .should().dependOnClassesThat().resideInAnyPackage(RulePackConfiguration.applicationServices())
                .because("Adapters must communicate through ports, not concrete services")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void onlyConfigurationMayDependOnServiceImplementations() {
        noClasses()
                .that().resideOutsideOfPackage("..configuration..")
                .and().resideOutsideOfPackage("..application.domain.service..")
                .should().dependOnClassesThat().resideInAnyPackage(RulePackConfiguration.applicationServices())
                .because("Concrete service implementations should be referenced only by configuration or by their own package")
                .allowEmptyShould(true)
                .check(classes);
    }
}