# JaCoCo Exclusion Report Gradle Plugin

A Gradle plugin that scans Java source files for the `@ExcludeFromJacocoGeneratedCodeCoverage`
annotation and generates a human-readable HTML and machine-readable XML audit report of every
excluded element.

The report gives teams full visibility into _what_ has been excluded from JaCoCo coverage
enforcement and _why_, without having to hunt through source code.

## Plugin ID

```
com.arc-e-tect.jacoco-exclusion-report
```

## Requirements

| Requirement       | Version |
|-------------------|---------|
| Gradle            | 8.x +   |
| Java              | 17 +    |
| JaCoCo (optional) | any bundled with Gradle |

## Applying the Plugin

### Using the `plugins` block (recommended)

**Groovy DSL (`build.gradle`)**

```groovy
plugins {
    id 'com.arc-e-tect.jacoco-exclusion-report' version '<version>'
}
```

**Kotlin DSL (`build.gradle.kts`)**

```kotlin
plugins {
    id("com.arc-e-tect.jacoco-exclusion-report") version "<version>"
}
```

## Annotation dependency

The plugin scans for `@ExcludeFromJacocoGeneratedCodeCoverage`, which is provided by the
`sedr-library` artifact. Add it as a `compileOnly` dependency so it is available at compile
time but is not included in your production JAR.

**Groovy DSL**

```groovy
dependencies {
    compileOnly 'com.arc_e_tect.book.sedr:sedr-library:<version>'
}
```

**Kotlin DSL**

```kotlin
dependencies {
    compileOnly("com.arc_e_tect.book.sedr:sedr-library:<version>")
}
```

JaCoCo 0.8.2 and later automatically ignores any element annotated with an annotation whose
simple name contains `"Generated"`. Because `ExcludeFromJacocoGeneratedCodeCoverage` satisfies
this condition, the annotated elements are excluded from both the JaCoCo coverage report _and_
from coverage verification thresholds without any additional configuration.

## Annotating source code

Apply the annotation at the narrowest appropriate scope to keep the audit list as small
and meaningful as possible.

**Class level** — excludes all methods within the class:

```java
import com.arc_e_tect.book.sedr.jacoco.marker.ExcludeFromJacocoGeneratedCodeCoverage;

@ExcludeFromJacocoGeneratedCodeCoverage(justification = "Abstract DSL — all implementations are Gradle-generated")
public abstract class MyExtension { ... }
```

**Method level** — preferred when only one or two methods are untestable:

```java
@Override
@ExcludeFromJacocoGeneratedCodeCoverage(justification = "Gradle plugin wiring — requires GradleTestKit to exercise")
public void apply(Project project) { ... }
```

**Constructor level**:

```java
@Inject
@ExcludeFromJacocoGeneratedCodeCoverage(justification = "Gradle task constructor — instantiated by infrastructure")
public MyTask() { ... }
```

The `justification` attribute is optional but strongly recommended. It is included verbatim in
both the HTML and XML reports and forms the documentary record of why each exclusion exists.

## Configuration

The plugin registers a `jacocoExclusionReport` extension that accepts the following properties.
All properties have sensible defaults and do not need to be set for a typical project.

**Groovy DSL**

```groovy
jacocoExclusionReport {
    // Simple (unqualified) name of the annotation to search for.
    // Default: 'ExcludeFromJacocoGeneratedCodeCoverage'
    annotationName = 'ExcludeFromJacocoGeneratedCodeCoverage'

    // Java source directories to scan.
    // Default: sourceSets.main.java.srcDirs (when the java plugin is applied)
    sourceDirs.from(sourceSets.main.java.srcDirs)

    // Directory where the HTML and XML reports are written.
    // Default: layout.buildDirectory.dir('reports/jacoco-exclusions')
    reportDir = layout.buildDirectory.dir('reports/jacoco-exclusions')
}
```

**Kotlin DSL**

```kotlin
jacocoExclusionReport {
    annotationName = "ExcludeFromJacocoGeneratedCodeCoverage"
    sourceDirs.from(sourceSets.main.java.srcDirs)
    reportDir = layout.buildDirectory.dir("reports/jacoco-exclusions")
}
```

## Task

| Task name               | Group        | Description |
|-------------------------|--------------|-------------|
| `jacocoExclusionReport` | verification | Scans sources for the exclusion annotation and writes HTML + XML reports. |

The task is automatically wired into the `check` lifecycle task. When `jacocoTestCoverageVerification`
is present (i.e. the JaCoCo plugin is applied), the exclusion report runs _before_ coverage
verification so both are always produced together.

Running the report explicitly:

```shell
./gradlew jacocoExclusionReport
```

Running as part of the standard verification suite:

```shell
./gradlew check
```

## Report output

After the task runs, two files are written to the configured `reportDir`
(default: `build/reports/jacoco-exclusions/`):

| File          | Description |
|---------------|-------------|
| `index.html`  | Human-readable report grouped by package → class → member, with a summary bar showing totals per element type. |
| `exclusions.xml` | Machine-readable JUnit-style XML listing every excluded element with its type, line number, source file, and justification. |

## Full example

```groovy
plugins {
    id 'java'
    id 'jacoco'
    id 'com.arc-e-tect.jacoco-exclusion-report' version '<version>'
}

dependencies {
    compileOnly 'com.arc_e_tect.book.sedr:sedr-library:<version>'
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required  = true
        html.required = true
    }
}

jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.9
            }
        }
    }
}

check.dependsOn jacocoTestCoverageVerification
```

With this setup, running `./gradlew check` will:

1. Compile and test the project
2. Generate the JaCoCo coverage report
3. Generate the exclusion audit report (`jacocoExclusionReport`)
4. Enforce the coverage threshold (`jacocoTestCoverageVerification`)

## License

This plugin is part of the *Software Engineering Done Right* project.
See [LICENSE](../../LICENSE) for details.
