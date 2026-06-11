# Library-Based Rule Packs Guide

## Overview

External rule packs let an architecture team publish shared ArchUnit rules once and reuse them across many services.
The Architecture Validator plugin consumes those JARs from `testArchitectureImplementation`.

## Rule-pack structure

A rule-pack JAR should package ordinary JUnit or ArchUnit test classes.
The current plugin discovers classes whose names end with `Test` from dependency directories and JARs.

## Publishing model

Package the rule pack as a normal Java library with `maven-publish`.
Give it a stable coordinate and publish it to the same repository your application builds can consume.

## Using a rule pack

```groovy
dependencies {
    testArchitectureImplementation 'com.org.ea:layer-validator:1.0.0'
}
```

The plugin scans that dependency, generates `ExternalRulePackSuite`, and executes the discovered packages via JUnit Platform.

## Turning off the built-in hexagonal test

If you want to run only external rule packs (including the Spring companion) and skip the built-in hexagonal rules, disable the built-in validator in the extension:

```groovy
architectureValidator {
    useBuiltInHexagonalRulePack = false
}
```

With this setting in place, `ExternalRulePackSuite` and any user-provided tests in `src/testArchitecture/java` still run, but the built-in vanilla hexagonal class is not generated or executed.

## Duplicate rules

When multiple rule packs contribute the same simple test class name, the plugin warns by default.
Set `failOnDuplicateRules = true` if those collisions should stop the build.

## Companion Spring rule pack

This repository ships `architecture-validator-hexagonal-spring-rules` as the built-in Spring companion artifact.
It is just another rule pack from the plugin’s perspective.
The artifact is published to Maven Central and GitHub Packages.
It does not replace the built-in generated `HexagonalArchitectureTest`.
Instead, the `testArchitecture` suite runs the vanilla generated hexagonal rules and then adds the Spring-specific companion tests from the external rule-pack JAR.

Enable it exactly like any other external rule pack:

```groovy
dependencies {
    testArchitectureImplementation 'com.arc-e-tect:architecture-validator-hexagonal-spring-rules:<version>'
}
```

The plugin passes the same package configuration to the Spring companion through system properties such as `architectureValidator.basePackage`, `architectureValidator.inPorts`, `architectureValidator.outPorts`, `architectureValidator.domainModel`, `architectureValidator.adapters`, and `architectureValidator.applicationServices`.
That means the companion evaluates the same hexagonal package boundaries as the built-in template, but with Spring-aware assertions.

Compared with the vanilla generated `HexagonalArchitectureTest`, the Spring companion adds these test classes:

- `SpringHexagonalArchitectureTest`: verifies that `@Controller` and `@RestController` classes only depend on in-ports, `@Service` classes do not bypass out-ports to reach repositories directly, `@Repository` classes are only accessed from out-ports, adapters, or configuration, and Spring stereotypes stay inside the declared hexagonal layers.
- `PortContractTest`: reinforces the port contract checks by asserting that input and output ports are interfaces and that port signatures only depend on Java core types plus the declared domain model.
- `DependencyDirectionTest`: adds Spring wiring direction rules so the core application layer stays independent of adapters, adapters do not depend on concrete application-service implementations, and only configuration or the implementation package itself may reference concrete services.
- `DomainIsolationTest`: adds stricter framework isolation by ensuring the domain model does not depend on Spring, Jakarta, JPA, Hibernate, or Jackson types, and by asserting that application-service packages do not carry Spring `@Service` stereotypes.

In practice, the built-in `HexagonalArchitectureTest` gives you the framework-agnostic baseline, while the Spring companion tightens that baseline for Spring applications by checking stereotype placement, repository access, dependency direction, and framework leakage.

## Enterprise guidance

Keep rule-pack tests generic.
Use package-pattern conventions and system properties instead of hard-coded service-specific package names so the same rule pack remains reusable across projects.