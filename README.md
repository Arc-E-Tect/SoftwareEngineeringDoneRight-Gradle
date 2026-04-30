# SoftwareEngineeringDoneRight-Gradle

[![License](https://img.shields.io/github/license/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle)](LICENSE)
[![Security Scan (jacoco-exclusion-report)](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/actions/workflows/jacoco-exclusion-report-security-scan.yml/badge.svg)](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/actions/workflows/jacoco-exclusion-report-security-scan.yml)
[![Security Scan (gherkin-to-asciidoc)](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/actions/workflows/gherkin-to-asciidoc-security-scan.yml/badge.svg)](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/actions/workflows/gherkin-to-asciidoc-security-scan.yml)

A collection of useful and usable Gradle plugins and utilities for software engineering projects.

## Projects

| Project | Description | Build | Release |
|---------|-------------|-------|---------|
| [jacoco-exclusion-report](jacoco-exclusion-report/README.adoc) | Gradle plugin that scans Java sources for `@ExcludeFromJacocoGeneratedCodeCoverage` and generates an HTML + XML audit report of every excluded element, giving teams full visibility into what has been excluded from JaCoCo coverage enforcement and why. | [![Build](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/actions/workflows/jacoco-exclusion-report-build.yml/badge.svg)](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/actions/workflows/jacoco-exclusion-report-build.yml) | [![Release](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/actions/workflows/jacoco-exclusion-report-release.yml/badge.svg)](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/actions/workflows/jacoco-exclusion-report-release.yml) |
| [gherkin-to-asciidoc](gherkin-to-asciidoc/README.adoc) | Gradle plugin that scans `.feature` files for `Scenario` and `Scenario Outline` titles and generates an AsciiDoc file listing all scenarios, providing a living index of behaviour specifications ready to embed in project documentation. | [![Build](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/actions/workflows/gherkin-to-asciidoc-build.yml/badge.svg)](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/actions/workflows/gherkin-to-asciidoc-build.yml) | [![Release](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/actions/workflows/gherkin-to-asciidoc-release.yml/badge.svg)](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/actions/workflows/gherkin-to-asciidoc-release.yml) |
