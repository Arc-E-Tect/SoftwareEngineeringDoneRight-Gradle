package com.arc_e_tect.example.scopesplit.validation;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "com.arc_e_tect.example.scopesplit.platform")
class PlatformPolicyConventionsTest {

    @ArchTest
    static final ArchRule PLATFORM_TYPES_SHOULD_END_WITH_POLICY = classes()
            .that().resideInAPackage("..platform..")
            .should().haveSimpleNameEndingWith("Policy")
            .because("Local testArchitecture tests can target package scopes different from generated plugin tests");
}
