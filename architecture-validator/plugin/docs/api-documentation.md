# API Documentation

## Plugin class

`ArchitectureValidatorPlugin` registers the extension, the generator task, and the `testArchitecture` suite.
It also wires report locations and the post-execution threshold check.

## Extension

`ArchitectureValidatorExtension` exposes the public DSL.

- `testDirectory`
- `basePackage`
- `failOnViolation`
- `maxAllowedViolations`
- `ignoreFailures`
- `failOnDuplicateRules`
- `useBuiltInHexagonalRulePack`
- `hexagonalArchitecture`

## Nested hexagonal extension

`HexagonalArchitectureExtension` contains these package-pattern lists.

- `inPorts`
- `outPorts`
- `domainModel`
- `adapters`
- `applicationServices`
- `commonPackages`

## Generator task

`GenerateArchitectureTestsTask` renders the built-in vanilla `HexagonalArchitectureTest` source file.
That generation is controlled by `useBuiltInHexagonalRulePack`.
It also scans the rule-pack classpath and generates `ExternalRulePackSuite` when dependency rule packs are present.

## Generated classes

- `HexagonalArchitectureTest` is the built-in vanilla hexagonal rule set.
- `ExternalRulePackSuite` is generated only when dependency rule packs are discovered.

## Reports

The plugin configures the `testArchitecture` suite to emit XML and HTML reports under `build/reports/architecture-validator/`.

## Companion Spring artifact

The companion `spring-rules` project in SoftwareEngineeringDoneRight-Library publishes `architecture-validator-hexagonal-spring-rules`.
Its rule classes read the same system properties that the main plugin passes into the `testArchitecture` task.
The artifact is consumed through `testArchitectureImplementation` like any other external rule pack.