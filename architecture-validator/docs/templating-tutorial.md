# Templating Tutorial

## Template location

The built-in vanilla rule set is generated from `src/main/resources/templates/HexagonalArchitectureTest.java.template`.
The task that performs the generation is `GenerateArchitectureTestsTask`.

## Available placeholders

The current template supports these placeholders.

- `${generatedPackage}` for the generated Java package.
- `${basePackage}` for the imported application root package.
- `${inPorts}` for the driving port package patterns.
- `${outPorts}` for the driven port package patterns.
- `${domainModel}` for the domain package patterns.
- `${adapters}` for the adapter package patterns.
- `${applicationServices}` for the application service package patterns.
- `${commonPackages}` for shared utility package patterns.

## How generation works

At execution time the task resolves the extension values, renders the template, and writes `HexagonalArchitectureTest.java` into `build/generated/testArchitecture/java`.
In the same pass it scans the rule-pack classpath and, when needed, writes `ExternalRulePackSuite.java`.

## Customizing the built-in template

You can change the rule shape by editing the template resource and the generation task together.
That keeps placeholder handling explicit and avoids hidden string interpolation rules.

## Adding new placeholders

1. Add a new property to `ArchitectureValidatorExtension`.
2. Propagate it into `GenerateArchitectureTestsTask`.
3. Add a replacement entry in the generation task.
4. Use the placeholder in the template.
5. Add a focused unit test that asserts the rendered Java source contains the expected rule fragment.

## Clean Architecture or Layered variants

The templating model is intentionally simple.
If a future requirement needs a second built-in style, add a second template and a selection property rather than making one template encode too many unrelated architecture styles.