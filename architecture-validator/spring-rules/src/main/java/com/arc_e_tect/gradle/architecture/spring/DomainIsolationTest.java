package com.arc_e_tect.gradle.architecture.spring;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class DomainIsolationTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RulePackConfiguration.basePackage());

    @Test
    void domainModelShouldOnlyDependOnJavaCoreAndDomainModel() {
        classes()
                .that().resideInAnyPackage(RulePackConfiguration.domainModel())
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(RulePackConfiguration.merge(
                        RulePackConfiguration.domainModel(),
                        "java.util..",
                        "java.lang..",
                        "java.time.."))
                .because("Domain model classes must stay framework free")
                .check(classes);
    }

    @Test
    void coreApplicationLayerShouldHaveNoFrameworkDependencies() {
        noClasses()
                .that().resideInAnyPackage(RulePackConfiguration.domainModel())
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta..",
                        "javax.persistence..",
                        "javax.validation..",
                        "org.hibernate..",
                        "com.fasterxml.jackson..")
                .because("Domain model must not depend on Spring or persistence frameworks")
                .check(classes);
    }

    @Test
    void applicationServicesShouldNotCarrySpringStereotypes() {
        noClasses()
                .that().resideInAnyPackage(RulePackConfiguration.applicationServices())
                .should().beAnnotatedWith("org.springframework.stereotype.Service")
                .because("Service implementations should remain plain Java and be wired explicitly")
                .allowEmptyShould(true)
                .check(classes);
    }
}