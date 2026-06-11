# Templating Tutorial

## Goal

This guide shows how to add a second built-in generated test template, using a layered architecture template as the example.
The important detail is that the current implementation does not support multiple built-in templates yet.
Today the generator always loads `HexagonalArchitectureTest.java.template` and always writes `HexagonalArchitectureTest.java`.

That means adding a layered template is a small feature, not just a documentation change.
You need to add a template selector to the DSL, teach the generator how to choose a template, wire any new placeholders, and cover the new branch with tests.

This tutorial describes the code changes required to reach that end state.
It does not claim that the current plugin already ships both built-in templates.

## End state after completing the tutorial

If you implement all steps in this guide, the plugin will expose two built-in template choices:

- `hexagonal` for modules that should generate the existing port-and-adapter rules
- `layered` for modules that should generate traditional layered architecture rules

That means the plugin can support different built-in rule styles in the same multi-module build.
For example, one microservice-oriented subproject could use the built-in hexagonal template while a legacy layered subproject uses the built-in layered template.

Example end-state configuration:

```groovy
project(':orders-service') {
	architectureValidator {
		builtInTemplate = 'hexagonal'
		basePackage = 'com.example.orders'
	}
}

project(':backoffice-webapp') {
	architectureValidator {
		builtInTemplate = 'layered'
		basePackage = 'com.example.backoffice'
	}
}
```

To make that end state easier to review, the repository now includes a companion source snapshot under `docs/templating-tutorial-example/`.
That directory shows what the changed files would look like after the layered built-in template support has been fully implemented.

## Current generation flow

The current built-in vanilla rule set is generated from `src/main/resources/templates/HexagonalArchitectureTest.java.template`.
The generation happens in `GenerateArchitectureTestsTask`.
The plugin wires that task from `ArchitectureValidatorPlugin`.

At execution time the task does three things:

1. It loads a template resource.
2. It replaces placeholders with values from the extension.
3. It writes generated Java source into `build/generated/testArchitecture/java`.

In the same pass it may also generate `ExternalRulePackSuite.java` for rule packs discovered on the `testArchitecture` classpath.
That external suite logic is separate from the built-in template choice and usually does not need to change when you add a new built-in architecture style.

## Step 1: Add a template selection property

Start in `ArchitectureValidatorExtension`.
Add a new property that selects the built-in architecture style.

Example:

```java
private final Property<String> builtInTemplate;

builtInTemplate = objects.property(String.class).convention("hexagonal");

public Property<String> getBuiltInTemplate() {
	return builtInTemplate;
}
```

Why this comes first:

- The generator currently has no way to distinguish between `hexagonal` and `layered`.
- A stable extension property gives you one explicit switch instead of encoding selection logic inside file names or package patterns.

If the layered template needs different package buckets than the hexagonal template, add those as new properties at the same time.
For example, a layered model may want `presentation`, `application`, `domain`, and `infrastructure` package patterns instead of `inPorts` and `outPorts`.

## Step 2: Wire the new property into the task

Open `ArchitectureValidatorPlugin` and pass the new extension property into `GenerateArchitectureTestsTask` during task registration.

Example:

```java
task.getBuiltInTemplate().set(extension.getBuiltInTemplate());
```

Do the same for any new layered placeholders you introduced in the extension.

This step matters because the task currently receives only the hexagonal inputs:

- `basePackage`
- `inPorts`
- `outPorts`
- `domainModel`
- `adapters`
- `applicationServices`
- `commonPackages`

If the task never sees the layered inputs, the template cannot render them.

## Step 3: Add the new template resource

Create a second template file under `src/main/resources/templates/`.

Example:

```text
src/main/resources/templates/LayeredArchitectureTest.java.template
```

Keep the structure parallel to the existing hexagonal template.
That makes the generator code much easier to read and test.

Example skeleton:

```java
package ${generatedPackage};

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class LayeredArchitectureTest {

	private static final String BASE_PACKAGE = "${basePackage}";

	private final JavaClasses classes = new ClassFileImporter()
			.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
			.importPackages(BASE_PACKAGE);

	@Test
	void presentationLayerShouldNotDependOnInfrastructure() {
		noClasses()
				.that().resideInAnyPackage(${presentation})
				.should().dependOnClassesThat().resideInAnyPackage(${infrastructure})
				.because("Presentation should not depend directly on infrastructure")
				.check(classes);
	}
}
```

The placeholder names in that example are only illustrations.
Use names that match the properties you actually add to the extension and task.

The complete end-state example template is also available in `docs/templating-tutorial-example/src/main/resources/templates/LayeredArchitectureTest.java.template`.

## Step 4: Teach `GenerateArchitectureTestsTask` to choose a template

This is the main code change.
Right now the task is hard-coded to a single template path and a single generated class name.

Today it effectively assumes:

- template path: `HexagonalArchitectureTest.java.template`
- output class: `HexagonalArchitectureTest.java`

For multiple built-in styles, refactor that into a small selection method.

One straightforward approach is to introduce a tiny descriptor object.

Example shape:

```java
private record TemplateSpec(String templatePath, String outputClassName) {
}

private TemplateSpec selectTemplate() {
	return switch (getBuiltInTemplate().get()) {
		case "layered" -> new TemplateSpec(
				"templates/LayeredArchitectureTest.java.template",
				"LayeredArchitectureTest");
		case "hexagonal" -> new TemplateSpec(
				"templates/HexagonalArchitectureTest.java.template",
				"HexagonalArchitectureTest");
		default -> throw new GradleException("Unsupported built-in template: " + getBuiltInTemplate().get());
	};
}
```

Then update `generate()` to:

1. choose the template spec
2. load the selected template
3. build the replacement map for that template
4. write the selected output class name

This is cleaner than stuffing both architectures into one template file with conditional placeholders.

The full end-state example task is available in `docs/templating-tutorial-example/src/main/java/com/arc_e_tect/gradle/architecture/GenerateArchitectureTestsTask.java`.

## Step 5: Add placeholders for the new template

The current generator builds one replacement map with these placeholders:

- `${generatedPackage}`
- `${basePackage}`
- `${inPorts}`
- `${outPorts}`
- `${domainModel}`
- `${adapters}`
- `${applicationServices}`
- `${commonPackages}`

For a layered template, add only the placeholders that the layered rules actually use.

Example:

```java
Map<String, String> replacements = Map.of(
		"${generatedPackage}", GENERATED_PACKAGE,
		"${basePackage}", escapeJava(getBasePackage().getOrElse("")),
		"${presentation}", javaArrayLiteral(getPresentation().get()),
		"${application}", javaArrayLiteral(getApplication().get()),
		"${domain}", javaArrayLiteral(getDomain().get()),
		"${infrastructure}", javaArrayLiteral(getInfrastructure().get())
);
```

If you keep both template styles in one task, it is usually better to split replacement-map creation into separate methods such as `hexagonalReplacements()` and `layeredReplacements()`.
That makes it obvious which placeholders belong to which template.

## Step 6: Decide whether runtime system properties also need to change

Generated source replacement and runtime system properties solve different problems.

- Template replacement is used while writing the generated Java source.
- System properties are used at test runtime by external rule packs such as the Spring companion rules.

If your new layered built-in template is fully self-contained, you may not need any new system properties.
If you also want external layered rule packs to consume the same package definitions at runtime, then update `ArchitectureValidatorPlugin` to add matching `testTask.systemProperty(...)` entries.

Do not add runtime properties unless something actually reads them.

## Step 7: Add focused tests before calling the work finished

The existing test anchor is `GenerateArchitectureTestsTaskTest`.
Follow that pattern.

For a layered template, add at least these tests:

1. A generation test that selects the layered template and asserts the generated file name and important rule text.
2. A regression test that keeps the existing hexagonal template path working.
3. A failure test for an unsupported template name, if you add a guarded switch.

Example assertions:

```java
assertThat(generatedFile).exists();
assertThat(Files.readString(generatedFile))
		.contains("LayeredArchitectureTest")
		.contains("Presentation should not depend directly on infrastructure");
```

If you added new extension properties, also extend the plugin-level tests so the new property has a default and is correctly wired.

The companion example directory also includes a final illustrative test file at `docs/templating-tutorial-example/src/test/java/com/arc_e_tect/gradle/architecture/GenerateArchitectureTestsTaskTest.java`.

## Step 8: Document the new DSL and run the generated suite

Once the generator works, update the user-facing docs.
At minimum:

- `README.adoc`
- `docs/user-guide.adoc`
- this tutorial

Add a small configuration example.

Example:

```groovy
architectureValidator {
	builtInTemplate = 'layered'
	basePackage = 'com.example.layered'
	layeredArchitecture {
		presentation = ['..web..', '..api..']
		application = ['..application..']
		domain = ['..domain..']
		infrastructure = ['..persistence..', '..messaging..']
	}
}
```

Then run the generated suite locally:

```bash
./gradlew testArchitecture
```

If you changed only docs, no build is needed.
If you changed the generator, a passing generation test is the minimum bar before updating the tutorial.

When those code changes are complete, the plugin has two built-in templates that can be selected per project or subproject.
Until then, the layered variant shown here remains a design and implementation guide rather than an available runtime option in the current released plugin.

## Recommended implementation shape

If you add more built-in templates over time, keep each concern separate:

- one extension property that selects the template
- one template resource per built-in architecture style
- one replacement-map method per template family
- one test per template branch

That structure scales much better than turning `HexagonalArchitectureTest.java.template` into a universal template with many unrelated placeholders.

## Checklist

Use this sequence when adding a new built-in template such as layered architecture:

1. Add a template selector property to `ArchitectureValidatorExtension`.
2. Add any new package-pattern properties required by the new architecture style.
3. Wire those properties from `ArchitectureValidatorPlugin` into `GenerateArchitectureTestsTask`.
4. Add the new `.template` resource under `src/main/resources/templates/`.
5. Refactor `GenerateArchitectureTestsTask` to select template path and output class name.
6. Add placeholder replacement logic for the new template.
7. Add runtime system properties only if some runtime rule pack needs them.
8. Add focused unit tests for generation and invalid selection.
9. Update user-facing docs with the new DSL and examples.
10. Run `./gradlew testArchitecture` or at least the touched generator tests before considering the feature complete.