package com.arc_e_tect.gradle.architecture.spring;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class SpringHexagonalArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RulePackConfiguration.basePackage());

        private String[] mergeAll(String[] first, String[] second, String[] third, String... fixed) {
                String[] merged = RulePackConfiguration.merge(first, fixed);
                merged = RulePackConfiguration.merge(second, merged);
                return RulePackConfiguration.merge(third, merged);
        }

        private String[] mergeAll(String[] first, String[] second, String... fixed) {
                String[] merged = RulePackConfiguration.merge(first, fixed);
                return RulePackConfiguration.merge(second, merged);
        }

    @Test
    void controllersShouldOnlyCallInPorts() {
        classes()
                .that().areAnnotatedWith("org.springframework.stereotype.Controller")
                .or().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(RulePackConfiguration.merge(
                        RulePackConfiguration.inPorts(),
                        "java..",
                        "org.springframework.."))
                .because("Spring controllers should only call in-ports to maintain Hexagonal Architecture")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void servicesShouldNotAccessRepositoriesDirectly() {
        classes()
                .that().areAnnotatedWith("org.springframework.stereotype.Service")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(mergeAll(
                        RulePackConfiguration.outPorts(),
                        RulePackConfiguration.applicationServices(),
                        RulePackConfiguration.domainModel(),
                        "java..",
                        "org.springframework.."))
                .because("Spring services should not access repositories directly; use out-ports instead")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void repositoriesShouldOnlyBeAccessedViaOutPorts() {
        classes()
                .that().areAnnotatedWith("org.springframework.stereotype.Repository")
                .should().onlyBeAccessed().byClassesThat()
                .resideInAnyPackage(mergeAll(
                        RulePackConfiguration.outPorts(),
                        RulePackConfiguration.adapters(),
                        "..configuration.."))
                .because("Spring repositories should only be accessed via out-ports")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void springComponentsShouldFollowHexagonalLayers() {
        classes()
                .that().areAnnotatedWith("org.springframework.stereotype.Component")
                .or().areAnnotatedWith("org.springframework.stereotype.Service")
                .or().areAnnotatedWith("org.springframework.stereotype.Repository")
                .should().resideInAnyPackage(mergeAll(
                        RulePackConfiguration.applicationServices(),
                        RulePackConfiguration.domainModel(),
                        RulePackConfiguration.adapters()))
                .because("Spring components should follow Hexagonal layers")
                .allowEmptyShould(true)
                .check(classes);
    }
}