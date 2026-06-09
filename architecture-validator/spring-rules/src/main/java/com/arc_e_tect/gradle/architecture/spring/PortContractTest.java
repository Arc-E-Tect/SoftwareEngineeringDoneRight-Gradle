package com.arc_e_tect.gradle.architecture.spring;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class PortContractTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RulePackConfiguration.basePackage());

    @Test
    void inputPortsShouldBeInterfaces() {
        classes()
                .that().resideInAnyPackage(RulePackConfiguration.inPorts())
                .should().beInterfaces()
                .because("Input ports must be interfaces")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void outputPortsShouldBeInterfaces() {
        classes()
                .that().resideInAnyPackage(RulePackConfiguration.outPorts())
                .should().beInterfaces()
                .because("Output ports must be interfaces")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void portsShouldOnlyDependOnJavaCoreAndDomainModel() {
        classes()
                .that().resideInAnyPackage(RulePackConfiguration.inPorts())
                .or().resideInAnyPackage(RulePackConfiguration.outPorts())
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(RulePackConfiguration.merge(
                        RulePackConfiguration.domainModel(),
                        "java.util..",
                        "java.lang.."))
                .because("Port signatures must not leak framework types into the core application")
                .allowEmptyShould(true)
                .check(classes);
    }
}