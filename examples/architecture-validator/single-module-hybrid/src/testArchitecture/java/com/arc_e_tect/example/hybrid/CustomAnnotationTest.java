package com.arc_e_tect.example.hybrid;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.arc_e_tect.example.hybrid")
class CustomAnnotationTest {

    @ArchTest
    static final ArchRule APPLICATION_SHOULD_NOT_DEPEND_ON_ADAPTERS = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..")
            .because("Annotation-style ArchUnit tests can run alongside generated rules");
}