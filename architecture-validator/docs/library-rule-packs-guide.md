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

## Duplicate rules

When multiple rule packs contribute the same simple test class name, the plugin warns by default.
Set `failOnDuplicateRules = true` if those collisions should stop the build.

## Companion Spring rule pack

This repository ships `architecture-validator-spring-rules` as the built-in Spring companion artifact.
It is just another rule pack from the plugin’s perspective.
Setting `useSpringRulePack = true` adds it to the `testArchitecture` suite automatically.

## Enterprise guidance

Keep rule-pack tests generic.
Use package-pattern conventions and system properties instead of hard-coded service-specific package names so the same rule pack remains reusable across projects.