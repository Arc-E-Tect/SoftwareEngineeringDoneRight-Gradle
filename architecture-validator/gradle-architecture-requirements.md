# Gradle Architecture Validator Plugin Requirements

## Overview
Create a **Gradle plugin named `architecture-validator`**, written in **Java**, that performs **architecture testing** using **ArchUnit**. The plugin must:
- Create a new **test suite** named `testArchitecture`.
- Generate **ArchUnit tests** based on a **user-provided configuration** (e.g., package names, class/interface names via regex).
- **Generated tests must be written to `build/generated/testArchitecture/java`** to ensure they are **not versioned in Git** (generated code should not be committed).
- Support **Hexagonal Architecture compliance testing** out of the box with **vanilla rules** (no framework-specific dependencies).
- **Include a built-in Spring-based Hexagonal Architecture rule pack** as an **external rule pack** (see [Spring Hexagonal Architecture Rule Pack](#spring-hexagonal-architecture-rule-pack) below).
- Allow users to **add custom ArchUnit tests** in a configurable directory (default: `src/testArchitecture/java`).
- Use **JvmTestSuite** for test execution.
- Generate **standard ArchUnit reports** (HTML/XML).
- Support **failOnViolation**, **maxAllowedViolations**, and **ignoreFailures** properties.
- **Enforce multi-module compatibility** (see [Multi-Module Enforcement Strategy](#multi-module-enforcement-strategy) below).
- Support **library-based rule packs** (see [Library-Based Rule Packs](#library-based-rule-packs) below).

---

## Technical Requirements

### 1. Language and Tooling
- **Plugin Name**: `architecture-validator`
- **Plugin Language**: **Java** (Groovy preferred over Kotlin for DSL and build scripts where relevant).
- **Java Version**: **Java 25** (ensure compatibility and use features accordingly).
- **Dependency Management**:
  - **Version Catalogs**: All Gradle dependencies **must** be defined using [Gradle Version Catalogs](https://docs.gradle.org/current/userguide/version_catalogs.html).
  - **refreshVersions Plugin**: Use the [`refreshVersions`](https://github.com/jmfayard/refreshVersions) plugin for dependency management.

---

### 2. Plugin Structure

- The plugin source code must be generated in `./architecture-validator`
- The examples mentioned in these requirements must be generated in `./examples/architecture-validator/` (e.g., `./examples/architecture-validator/single-module-vanilla`, `./examples/architecture-validator/multi-module`, etc.).

#### A. Plugin Class
- **Name**: `ArchitectureValidatorPlugin`
- **Language**: **Java**
- **Purpose**: Registers the plugin, extensions, tasks, and configurations.
- **Example**:
  ```java
  public class ArchitectureValidatorPlugin implements Plugin<Project> {
      @Override
      public void apply(Project project) {
          // Register extension, task, and testArchitectureImplementation configuration
      }
  }
  ```

---

### 3. Configuration DSL
- **Extension Name**: `architectureValidator`
- **Language**: **Groovy** (preferred for DSL)
- **Purpose**: Allows users to configure:
  - Package/class name patterns (regex).
  - Custom test directory (default: `src/testArchitecture/java`).
  - Hexagonal Architecture settings (in/out ports, domain model, adapters, application services).
  - Test execution settings (`failOnViolation`, `maxAllowedViolations`, `ignoreFailures`).

#### **Good Example**:
```groovy
architectureValidator {
    testDirectory = file("src/customArchTests")
    hexagonalArchitecture {
        inPorts = ["com.example.ports.in..*"].asImmutable()
        outPorts = ["com.example.ports.out..*"].asImmutable()
        domainModel = ["com.example.domain..*"].asImmutable()
        adapters = ["com.example.adapters..*"].asImmutable()
        applicationServices = ["com.example.application..*"].asImmutable()
    }
    failOnViolation = true
    maxAllowedViolations = 0
    ignoreFailures = false
}
```

#### **Bad Example**:
```groovy
// Avoid: Hardcoded paths, no regex support, missing Hexagonal Architecture config
architectureValidator {
    testDirectory = "src/test" // Hardcoded, not flexible
    failOnViolation = true
}
```

---

### 4. Built-in Rule Sets

#### A. **Vanilla Hexagonal Architecture Rules**
- **Purpose**: Default, framework-agnostic rules for Hexagonal Architecture compliance.
- **Generated Class**: `HexagonalArchitectureTest` (written to `build/generated/testArchitecture/java`).
- **Rules**:
  - In-ports should only be called by application services.
  - Out-ports should not access in-ports.
  - Domain model should not depend on adapters.
  - Application services should not directly access adapters.

#### **Example Generated Test**:
```java
@AnalyzeClasses(packages = "${basePackage}")
public class HexagonalArchitectureTest {
    @ArchTest
    static final ArchRule IN_PORTS_SHOULD_ONLY_BE_CALLED_BY_APPLICATION_SERVICES =
        classes().that().resideInAPackage("${inPorts}")
            .should().onlyBeAccessed().byClassesThat()
            .resideInAPackage("${applicationServices}")
            .because("In-ports should only be called by application services to enforce Hexagonal Architecture.");

    @ArchTest
    static final ArchRule OUT_PORTS_SHOULD_NOT_ACCESS_IN_PORTS =
        classes().that().resideInAPackage("${outPorts}")
            .should().notAccessClassesThat()
            .resideInAPackage("${inPorts}")
            .because("Out-ports should not access in-ports to maintain separation of concerns.");

    @ArchTest
    static final ArchRule DOMAIN_SHOULD_NOT_DEPEND_ON_ADAPTERS =
        classes().that().resideInAPackage("${domainModel}")
            .should().notDependOnClassesThat()
            .resideInAPackage("${adapters}")
            .because("Domain model should not depend on adapters.");

    @ArchTest
    static final ArchRule APPLICATION_SHOULD_NOT_ACCESS_ADAPTERS_DIRECTLY =
        classes().that().resideInAPackage("${applicationServices}")
            .should().notAccessClassesThat()
            .resideInAPackage("${adapters}")
            .because("Application services should not directly access adapters; use out-ports instead.");
}
```

---

#### B. **Spring Hexagonal Architecture Rule Pack**
- **Purpose**: **External rule pack** for Spring-based applications, enforcing **Spring-specific Hexagonal Architecture rules** (e.g., `@Service`, `@Repository`, `@Controller` stereotypes).
- **Packaging**: This rule pack **must** be **included as a separate JAR** (e.g., `architecture-validator-spring-rules.jar`) and published alongside the plugin.
- **Dependency**: Users can add this rule pack to their project via the `testArchitectureImplementation` configuration.
- **Rules**:
  - **Controllers should only call in-ports** (not application services or adapters directly).
  - **Services should not access repositories directly** (use out-ports).
  - **Repositories should only be accessed via out-ports** (not directly by services or controllers).
  - **Spring components should follow Hexagonal layers** (e.g., `@Service` in application, `@Repository` in adapters).

#### **Example Rule Pack Content**
The `architecture-validator-spring-rules.jar` **must** include the following ArchUnit tests:

```java
@AnalyzeClasses(packages = "${basePackage}")
public class SpringHexagonalArchitectureTest {

    @ArchTest
    static final ArchRule CONTROLLERS_SHOULD_ONLY_CALL_IN_PORTS =
        classes().that().areAnnotatedWith(Controller.class)
            .should().onlyAccessClassesThat()
            .resideInAPackage("${inPorts}")
            .because("Spring controllers should only call in-ports to maintain Hexagonal Architecture.");

    @ArchTest
    static final ArchRule SERVICES_SHOULD_NOT_ACCESS_REPOSITORIES_DIRECTLY =
        classes().that().areAnnotatedWith(Service.class)
            .should().notAccessClassesThat()
            .areAnnotatedWith(Repository.class)
            .because("Spring services should not access repositories directly; use out-ports instead.");

    @ArchTest
    static final ArchRule REPOSITORIES_SHOULD_ONLY_BE_ACCESSED_VIA_OUT_PORTS =
        classes().that().areAnnotatedWith(Repository.class)
            .should().onlyBeAccessed().byClassesThat()
            .resideInAPackage("${outPorts}")
            .because("Spring repositories should only be accessed via out-ports.");

    @ArchTest
    static final ArchRule SPRING_COMPONENTS_SHOULD_FOLLOW_HEXAGONAL_LAYERS =
        classes().that().areAnnotatedWith(Component.class)
            .should().resideInAnyPackage("${applicationServices}", "${domainModel}", "${adapters}")
            .because("Spring components should reside in application, domain, or adapter layers.");
}
```

#### **How to Use the Spring Rule Pack**
Users can include the Spring rule pack in their project by adding it as a dependency:
```groovy
dependencies {
    testArchitectureImplementation 'com.org.architecture-validator:architecture-validator-spring-rules:1.0.0'
}
```

---

### 5. Generated Classes
The plugin **must generate** the following classes dynamically based on the configuration. These classes:
- **Must be written to `build/generated/testArchitecture/java`** to ensure they are **not versioned in Git**.
- **Must have human-friendly names** and include **descriptive rule names** for clarity in reports.

#### A. **Hexagonal Architecture Test Class**
- **Name**: `HexagonalArchitectureTest`
- **Location**: `build/generated/testArchitecture/java` (automatically added to the `testArchitecture` source set).
- **Purpose**: Contains **pre-defined ArchUnit rules** for Hexagonal Architecture compliance (vanilla rules).

#### B. **Custom Test Support**
- Users can add their own ArchUnit tests in the configured directory (default: `src/testArchitecture/java`).
- The plugin **must compile and run** both generated and user-provided tests.
- Custom tests must follow the same **naming and documentation conventions** as generated tests.
- Users can use **either annotations or imperative style** for their custom tests.

---

### 6. Library-Based Rule Packs
The plugin **must** support loading additional architecture validation rules from **external JAR dependencies** (e.g., `layer-validator.jar`). This enables:
- **Organization-wide architecture rules** defined by an Enterprise Architecture (EA) Team.
- **Reusable rule packs** published to an internal Maven repository.

#### **How It Works**
1. **EA Team**:
   - Defines **organization-wide architecture rules** (e.g., Hexagonal, Clean, Layered).
   - Packages them in a **JAR** (e.g., `layer-validator.jar`) with ArchUnit tests (annotation or imperative).
   - Publishes the JAR to an **internal Maven repository**.

2. **Project Teams**:
   - Add the JAR as a dependency to the **`testArchitectureImplementation`** configuration.
   - The plugin **automatically discovers and runs** the tests from the JAR.

#### **Dependency Configuration**
Users can add library-based rule packs as dependencies in their `build.gradle`:
```groovy
dependencies {
    testArchitectureImplementation 'com.org.ea:layer-validator:1.0.0'
    testArchitectureImplementation 'com.org.ea:hexagonal-rules:2.0.0'
}
```

#### **Classpath Scanning**
The plugin **must**:
1. Resolve the **`testArchitectureImplementation`** configuration and its dependencies.
2. Scan the **classpath** for ArchUnit tests in:
   - The configured test directory (e.g., `src/testArchitecture/java`).
   - **All JARs** in the `testArchitectureImplementation` configuration.
3. Run all discovered tests using **JvmTestSuite**.

#### **Example Rule Pack JAR Structure**
```
layer-validator.jar
├── com/
│   └── org/
│       └── ea/
│           ├── HexagonalRulesTest.class (annotation-based)
│           ├── LayeredRulesTest.class (imperative)
│           └── CleanArchitectureRulesTest.class (annotation-based)
└── META-INF/
    └── architecture-rules.json (optional metadata)
```

#### **Handling Duplicates**
- If the same rule is defined in **multiple JARs** or in the **project’s test directory**, the plugin **must**:
  - Log a **warning** (but not fail the build).
  - Run **all instances** of the rule (or allow users to configure behavior via `failOnDuplicateRules`).

---

### 7. Test Execution
- **Task Name**: `testArchitecture`
- **Purpose**: Compiles and runs all ArchUnit tests (generated + user-provided + library-based).
- **Requirements**:
  - Use **JvmTestSuite** for test execution.
  - Support **failOnViolation** (default: `true`).
  - Support **maxAllowedViolations** (default: `0`).
  - Support **ignoreFailures** (default: `false`). If `true`, the task will not fail the build even if violations are found.
  - Generate **standard ArchUnit reports** (HTML/XML) in `${buildDir}/reports/architecture-validator/`.

#### **Good Example**:
```groovy
tasks.register('testArchitecture', TestArchitectureTask) {
    it.extension = extension
    it.failOnViolation = extension.failOnViolation
    it.maxAllowedViolations = extension.maxAllowedViolations
    it.ignoreFailures = extension.ignoreFailures
}
```

#### **Bad Example**:
```groovy
// Avoid: No support for failOnViolation, maxAllowedViolations, or ignoreFailures
tasks.register('testArchitecture') {
    // Missing configuration
}
```

---

### 8. Reports
- **Output**: Standard ArchUnit reports (HTML/XML).
- **Location**: `${buildDir}/reports/architecture-validator/`
- **Content**:
  - Clear **human-readable descriptions** of violations.
  - **Rule names** that match the generated test class (e.g., `InPortsShouldOnlyBeCalledByApplicationServices`).
  - **File and line numbers** for each violation.
- **Example**:
  ```
  build/
    reports/
      architecture-validator/
        index.html
        report.xml
  ```

---

### 9. Dependencies
- **Version Catalogs**: All dependencies **must** be defined in `libs.versions.toml`.
- **refreshVersions**: Use the `refreshVersions` plugin to manage dependency updates.
- **Required Dependencies**:
  - `org.archunit:archunit` (latest stable version)
  - `org.junit.jupiter:junit-jupiter-api` (latest stable version)
  - **Spring Rule Pack** (optional, for Spring-based projects):
    - `org.springframework:spring-context` (for Spring annotations like `@Service`, `@Repository`, etc.).

---

## Multi-Module Enforcement Strategy

### **What It Means**
The plugin **must** be designed to work seamlessly in **multi-module Gradle projects**. This requires:

1. **Isolated Test Execution**:
   - Each submodule **must** be able to run `testArchitecture` independently.
   - The plugin **must** respect the module’s own dependencies and classpath.

2. **Configuration Inheritance**:
   - Allow **global configuration** in the root project (e.g., default Hexagonal Architecture patterns).
   - Allow **module-specific overrides** (e.g., a submodule can define its own `inPorts` or `outPorts`).

3. **Cross-Module Analysis**:
   - The plugin **must** support analyzing dependencies **across modules** (e.g., ensure that `application` modules do not violate architecture rules when accessing `domain` modules).
   - Use Gradle’s `sourceSets` and `configurations` to resolve classes from all relevant modules.

4. **Report Aggregation**:
   - Generate a **consolidated report** for the entire project (optional).
   - Each module **must** generate its own report by default.

#### **Example Multi-Module Structure**:
```
root/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── libs.versions.toml
├── app/
│   ├── build.gradle
│   └── src/
│       ├── main/java/
│       └── testArchitecture/java/
├── domain/
│   ├── build.gradle
│   └── src/
│       ├── main/java/
│       └── testArchitecture/java/
└── adapters/
    ├── build.gradle
    └── src/
        ├── main/java/
        └── testArchitecture/java/
```

#### **Good Example (Root `build.gradle`)**:
```groovy
plugins {
    id 'com.github.jmfayard.refreshVersions' version '0.50.0'
}

// Apply plugin to all subprojects
subprojects {
    apply plugin: 'architecture-validator'
    
    architectureValidator {
        // Global defaults
        hexagonalArchitecture {
            inPorts = ["..ports.in..*"].asImmutable()
            outPorts = ["..ports.out..*"].asImmutable()
        }
    }
}
```

#### **Good Example (Submodule `build.gradle`)**:
```groovy
architectureValidator {
    // Override for this module
    hexagonalArchitecture {
        inPorts = ["com.app.ports.in..*"].asImmutable()
    }
}
```

---

## Testing Requirements
- **Unit Tests**:
  - **Minimum Coverage**: **90%** (striving for **100%**).
  - **Tools**: Use JaCoCo or similar for coverage reporting.
  - **Scope**: Test all plugin logic, including:
    - Configuration parsing.
    - Test generation.
    - Report generation.
    - Multi-module compatibility.
    - Library-based rule pack discovery.

- **Integration Tests**:
  - Test the plugin in a **real multi-module project**.
  - Verify that:
    - Generated tests run correctly.
    - Reports are generated.
    - `failOnViolation`, `maxAllowedViolations`, and `ignoreFailures` work as expected.
    - Library-based rule packs are discovered and executed.

---

## Publishing Requirements
- **Gradle Plugin Portal**:
  - The plugin **must** be published to the [Gradle Plugin Portal](https://plugins.gradle.org/) with the **ID `architecture-validator`**.
  - Include clear documentation and usage examples.

- **Local Registry**:
  - The plugin **must** also be published to a **local Maven repository** (e.g., `~/m2/repository`) for testing and example project generation.
  - Use the `maven-publish` plugin for this purpose.

- **Spring Rule Pack**:
  - The **Spring Hexagonal Architecture rule pack** (e.g., `architecture-validator-spring-rules.jar`) **must** be published alongside the plugin.
  - It **must** be available in:
    - The **Gradle Plugin Portal** (as a companion artifact).
    - A **local Maven repository** for testing.

---

## Example Projects
The plugin **must** include **five example projects** to demonstrate its usage:

### 1. **Single-Module (Vanilla Hexagonal)**
- **Purpose**: Demonstrate the plugin in a **simple, single-module Gradle project** with **vanilla Hexagonal Architecture rules**. 
- **Requirements**:
  - The project **must** build successfully.
  - Running `testArchitecture` **must** generate ArchUnit reports.
  - The project **must** include **intentional architecture violations** (e.g., a class in `application` directly accessing a class in `adapters`).
  - The **README** **must** explain:
    - What violations exist.
    - How to observe them in the generated report.
    - How to fix them.

#### **Example Violation**:
```java
// In the single-module project's application package
public class ApplicationService {
    // Violation: Directly accessing an adapter (should use ports)
    private final DatabaseAdapter adapter = new DatabaseAdapter();
}
```

---

### 2. **Single-Module (Spring Hexagonal)**
- **Purpose**: Demonstrate the plugin with the **Spring Hexagonal Architecture rule pack**. 
- **Requirements**:
  - The project **must** build successfully.
  - Running `testArchitecture` **must** generate ArchUnit reports including **Spring-specific violations**.
  - The project **must** include the Spring rule pack as a dependency:
    ```groovy
    dependencies {
        testArchitectureImplementation 'com.org.architecture-validator:architecture-validator-spring-rules:1.0.0'
    }
    ```
  - The project **must** include **intentional Spring-specific violations** (e.g., a `@Service` directly accessing a `@Repository`).
  - The **README** **must** explain:
    - What Spring-specific violations exist.
    - How to observe them in the generated report.
    - How to fix them.

#### **Example Violation**:
```java
// In the single-module Spring project
@Service
public class OrderService {
    // Violation: Directly accessing a repository (should use out-ports)
    @Autowired
    private OrderRepository orderRepository;
}
```

---

### 3. **Single-Module (Hybrid)**
- **Purpose**: Demonstrate the plugin with **both annotation and imperative user-provided tests**. 
- **Requirements**:
  - The project **must** build successfully.
  - Running `testArchitecture` **must** generate ArchUnit reports.
  - The project **must** include:
    - **Generated tests** (annotations, in `build/generated/testArchitecture/java`).
    - **User-provided annotation-based tests** (in `src/testArchitecture/java`).
    - **User-provided imperative tests** (in `src/testArchitecture/java`).
  - The **README** **must** explain:
    - How **both styles** (annotations and imperative) can coexist.
    - Side-by-side examples of the same rule in both styles.

#### **Example Files**:
```
single-module-hybrid/
├── build.gradle
├── src/
│   ├── main/java/
│   │   └── com/example/
│   │       ├── ApplicationService.java
│   │       └── DatabaseAdapter.java
│   └── testArchitecture/java/
│       ├── CustomAnnotationTest.java (user-provided, annotations)
│       └── CustomImperativeTest.java (user-provided, imperative)
└── README.md
```

---

### 4. **Multi-Module**
- **Purpose**: Demonstrate the plugin in a **multi-module Gradle project** with cross-module dependencies.
- **Requirements**:
  - The project **must** build successfully.
  - Running `testArchitecture` **must** generate ArchUnit reports for **each module** and a **consolidated report** for the entire project.
  - The project **must** include **intentional cross-module architecture violations** (e.g., a class in the `app` module directly accessing a class in the `adapters` module).
  - The **README** **must** explain:
    - What violations exist.
    - How to observe them in the generated reports.
    - How to fix them.

#### **Example Violation**:
```java
// In the app module
public class AppService {
    // Violation: Directly accessing an adapter from another module
    private final DatabaseAdapter adapter = new DatabaseAdapter();
}
```

---

### 5. **Library-Based Rules**
- **Purpose**: Demonstrate how to **include a custom rule pack JAR** (e.g., `layer-validator.jar`) and run its tests.
- **Requirements**:
  - The project **must** build successfully.
  - Running `testArchitecture` **must** generate ArchUnit reports including violations from the library-based rules.
  - The project **must** include a **dependency on a rule pack JAR** (e.g., `layer-validator.jar`).
  - The **README** **must** explain:
    - How to include a rule pack JAR.
    - How the plugin discovers and runs tests from the JAR.
    - How an **EA Team** can create and publish rule packs.

#### **Example `build.gradle`**:
```groovy
dependencies {
    testArchitectureImplementation 'com.org.ea:layer-validator:1.0.0'
}
```

---

## Documentation Requirements
The plugin **must** include **extensive documentation** to help users understand and customize its behavior. This includes:

### 1. **User Guide**
- **Purpose**: Explain how to **use the plugin** in a project.
- **Content**:
  - **Installation**: How to apply the plugin (Gradle Plugin Portal, local registry).
  - **Configuration**: How to configure the plugin (e.g., `architectureValidator` DSL).
  - **Running Tests**: How to run `testArchitecture` and interpret the reports.
  - **Handling Violations**: How to address common architecture violations.
  - **Generated Tests**: Explanation that generated tests are written to `build/generated/testArchitecture/java` and should **not** be versioned in Git.

### 2. **Templating System Tutorial**
- **Purpose**: Explain how to **customize the templating system** to generate custom ArchUnit rules.
- **Content**:
  - **Template Structure**: Overview of the template files (e.g., `HexagonalArchitectureTest.java.template`).
  - **Placeholders**: List of supported placeholders (e.g., `${inPorts}`, `${outPorts}`, `${ruleDescription}`).
  - **Custom Rules**: How to add custom ArchUnit rules to the template.
  - **Examples**: Step-by-step examples of customizing the template for different architecture patterns (e.g., Clean Architecture, Layered Architecture).

#### **Example Template Customization**:
```java
// Custom template for Clean Architecture
@AnalyzeClasses(packages = "${basePackage}")
public class CleanArchitectureTest {
    @ArchTest
    static final ArchRule USE_CASES_SHOULD_NOT_ACCESS_FRAMEWORKS =
        classes().that().resideInAPackage("${useCases}")
            .should().notAccessClassesThat()
            .resideInAPackage("${frameworks}")
            .because("Use cases should not depend on frameworks to maintain Clean Architecture.");
}
```

### 3. **Library-Based Rule Packs Guide**
- **Purpose**: Explain how to **create, publish, and use** library-based rule packs.
- **Content**:
  - **Creating a Rule Pack**: How to structure and package a JAR with ArchUnit tests.
  - **Publishing a Rule Pack**: How to publish the JAR to an internal Maven repository.
  - **Using a Rule Pack**: How to add the JAR as a `testArchitectureImplementation` dependency.
  - **Enterprise Use Case**: How an **EA Team** can enforce organization-wide rules.
  - **Spring Rule Pack**: How to use the **built-in Spring Hexagonal Architecture rule pack**.

### 4. **API Documentation**
- **Purpose**: Document the **plugin’s API** for advanced users.
- **Content**:
  - **Extension Properties**: List and description of all configurable properties (e.g., `failOnViolation`, `maxAllowedViolations`, `ignoreFailures`).
  - **Task Properties**: List and description of all task properties.
  - **Generated Classes**: Description of the classes generated by the plugin (e.g., `HexagonalArchitectureTest`).

---

## Success Criteria
1. The plugin **builds successfully** in both single-module and multi-module projects.
2. The plugin **generates ArchUnit tests** in `build/generated/testArchitecture/java` based on the configuration.
3. The plugin **runs tests and generates reports** (HTML/XML).
4. The plugin **respects `failOnViolation`, `maxAllowedViolations`, and `ignoreFailures`**.
5. The plugin **works in multi-module projects** with isolated and cross-module analysis.
6. The plugin **has 90%+ unit test coverage** (striving for 100%).
7. The plugin **is published to the Gradle Plugin Portal with the ID `architecture-validator` and to a local registry**.
8. The **Spring Hexagonal Architecture rule pack** is published alongside the plugin.
9. The **single-module (vanilla), single-module (Spring), single-module (hybrid), multi-module, and library-based example projects build, run tests, and demonstrate violations in the reports**.
10. The **documentation is complete**, including the **user guide**, **templating system tutorial**, and **library-based rule packs guide**.

---

## Deliverables
1. **Plugin Source Code**:
   - `build.gradle` (Groovy, using version catalogs and `refreshVersions`).
   - `ArchitectureValidatorPlugin.java` (plugin class).
   - `ArchitectureValidatorExtension.java` (configuration DSL).
   - `TestArchitectureTask.java` (custom task).
   - `HexagonalArchitectureTest.java.template` (template for generated vanilla Hexagonal tests).
   - `libs.versions.toml` (version catalog).

2. **Spring Rule Pack**:
   - `SpringHexagonalArchitectureTest.java` (Spring-specific rules).
   - Published as a **separate JAR** (e.g., `architecture-validator-spring-rules.jar`).

3. **Documentation**:
   - **User Guide**: Usage instructions, configuration options, and multi-module setup guide.
   - **Templating System Tutorial**: How to customize templates and add custom rules.
   - **Library-Based Rule Packs Guide**: How to create, publish, and use rule packs (including the Spring rule pack).
   - **API Documentation**: Extension properties, task properties, and generated classes.
   - Example `build.gradle` for users.

4. **Tests**:
   - Unit tests (90%+ coverage).
   - Integration tests (single-module, multi-module, and library-based).

5. **Example Projects**:
   - **Single-Module (Vanilla Hexagonal)**: Demonstrates the plugin with vanilla Hexagonal Architecture rules.
   - **Single-Module (Spring Hexagonal)**: Demonstrates the plugin with the Spring rule pack.
   - **Single-Module (Hybrid)**: Demonstrates the plugin with both annotation and imperative user tests.
   - **Multi-Module**: Demonstrates the plugin in a multi-module project with cross-module violations.
   - **Library-Based Rules**: Demonstrates the plugin with a custom rule pack JAR dependency.
   - All projects include a **README** explaining the violations and how to observe them in the reports.

6. **Publication Artifacts**:
   - Plugin published to Gradle Plugin Portal with the **ID `architecture-validator`**. 
   - Plugin published to a local Maven repository.
   - Spring rule pack published alongside the plugin.

---

## Example Project Structure
```
architecture-validator/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── libs.versions.toml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── ArchitectureValidatorPlugin.java
│   │   │   ├── ArchitectureValidatorExtension.java
│   │   │   └── TestArchitectureTask.java
│   │   └── resources/
│   │       └── templates/
│   │           └── HexagonalArchitectureTest.java.template
│   └── test/
│       └── java/
│           ├── ArchitectureValidatorPluginTest.java
│           └── TestArchitectureTaskTest.java
├── spring-rules/
│   ├── build.gradle
│   └── src/
│       └── main/java/
│           └── SpringHexagonalArchitectureTest.java
├── docs/
│   ├── user-guide.md
│   ├── templating-tutorial.md
│   ├── library-rule-packs-guide.md
│   └── api-documentation.md
├── example-projects/
│   ├── single-module-vanilla/
│   │   ├── build.gradle
│   │   ├── settings.gradle
│   │   ├── src/
│   │   │   ├── main/java/
│   │   │   └── testArchitecture/java/
│   │   │       └── CustomAnnotationTest.java
│   │   └── README.md
│   ├── single-module-spring/
│   │   ├── build.gradle
│   │   ├── settings.gradle
│   │   ├── src/
│   │   │   ├── main/java/
│   │   │   └── testArchitecture/java/
│   │   └── README.md
│   ├── single-module-hybrid/
│   │   ├── build.gradle
│   │   ├── settings.gradle
│   │   ├── src/
│   │   │   ├── main/java/
│   │   │   └── testArchitecture/java/
│   │   │       ├── CustomAnnotationTest