# Architecture Validator User Guide

## Installation

Apply the plugin with `com.arc-e-tect.architecture-validator`.
The examples in this repository consume it through a composite build, but normal projects can use the Gradle Plugin Portal once the plugin is published.

## Core workflow

The plugin creates a `testArchitecture` suite.
That suite compiles generated tests from `build/generated/testArchitecture/java`, user tests from `src/testArchitecture/java`, and any external rule packs added to `testArchitectureImplementation`.

## Typical configuration

```groovy
plugins {
    id 'java'
    id 'com.arc-e-tect.architecture-validator' version '<version>'
}

architectureValidator {
    basePackage = 'com.example.order'
    failOnViolation = true
    maxAllowedViolations = 0
    ignoreFailures = false

    hexagonalArchitecture {
        inPorts = ['..application.port.in..']
        outPorts = ['..application.port.out..']
        domainModel = ['..application.domain..']
        adapters = ['..adapter..', '..adapters..']
        applicationServices = ['..application.service..']
        commonPackages = ['..application.common..']
    }
}
```

## Reports

Run `./gradlew testArchitecture`.
The suite emits HTML and XML reports under `build/reports/architecture-validator/`.
The Gradle HTML report is the fastest way to inspect which rule failed and which classes triggered the failure.

## Generated tests

The plugin always regenerates its built-in tests under `build/generated/testArchitecture/java`.
Those sources are derived artifacts and must not be versioned.

If you want to extend the plugin with an additional built-in generated template, see `docs/templating-tutorial.md`.
That tutorial now also points to `docs/templating-tutorial-example/`, which shows the final end-state source files for a two-template design with both `hexagonal` and `layered` built-in options.

## Handling violations

Set `ignoreFailures = true` when the suite should report but not fail the build.
Use `maxAllowedViolations` when teams want a gradual ratchet instead of an immediate zero-violation gate.

## Spring rule pack

Set `useSpringRulePack = true` to add the companion Spring rule pack automatically.
You can also add the dependency manually to `testArchitectureImplementation`.

## Multi-module builds

Apply the plugin to each Java subproject.
Keep shared conventions in the root build and override patterns only in the modules that differ from the default layout.