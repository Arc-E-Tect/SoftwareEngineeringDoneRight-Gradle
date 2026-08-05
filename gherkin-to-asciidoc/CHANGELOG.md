# [3.0.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/v2.1.0...v3.0.0) (2026-08-05)


### ✨ New and updated features

* **gherkin-to-asciidoc:** add forceRewrite to skip renumbering already-numbered lines ([#103](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/103)) ([48a74de](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/48a74decfe2c73dfdefca747a65922756820984a)), closes [#103](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/103)


### BREAKING CHANGE

* **gherkin-to-asciidoc:** indexing's default numbering behaviour changes.
Previously every generateFeatureDocs run fully renumbered every
Feature/Scenario from scratch; by default it now preserves numbers
that already match the current indexing value's format instead. Set
forceRewrite = true (or -PgherkinToAsciidoc.forceRewrite=true) to
keep the old always-renumber-everything behaviour.

# [2.1.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/v2.0.0...v2.1.0) (2026-08-05)


### ✨ New and updated features

* **gherkin-to-asciidoc:** add ci indexing value and a CLI override for the whole build ([#102](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/102)) ([063c069](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/063c069e406f4b2ffe7f40016a4af6ceb45d2999)), closes [#102](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/102) [#99](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/99)


### 📝 Documentation

* **gherkin-to-asciidoc:** add multi-project example demonstrating the indexing modes ([#100](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/100)) ([a0cd7a7](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/a0cd7a7c481fdd11079b77199a16c4b43c8b1104)), closes [#100](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/100)
* **gherkin-to-asciidoc:** document the indexing property and the includeSubDirs/groupByFeature default changes ([#101](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/101)) ([9fe4afd](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/9fe4afd23b5ee85ab95f322e4115cc67129dd2ac)), closes [#101](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/101) [#99](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/99)

# [2.0.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/v1.6.0...v2.0.0) (2026-08-04)


### ✨ New and updated features

* **example-shadow-api-detector:** add composed RequestMapping example ([#98](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/98)) ([6ea5163](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/6ea5163e61c1fd8d271b356e0bbdff6e48f459c1)), closes [#98](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/98)
* **gherkin-to-asciidoc:** add indexing DSL property to number features and scenarios ([#99](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/99)) ([5c207c4](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/5c207c4e0052499c6b796a3b7640a7b13e7b676f)), closes [#99](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/99)
* **shadow-api-detector:** add OpenAPI 3.2 compatibility support and example ([#95](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/95)) ([c57432b](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/c57432b4fc73fc9043291edef634ea7ba22d89e7)), closes [#95](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/95)


### 🐛 Bug Fixes

* **gherkin-to-asciidoc:** set systemUnderTestVersion in snippet-templates example ([#94](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/94)) ([ca7fb34](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/ca7fb346bd41113294928ab441ba0099fcf4301a)), closes [#94](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/94)
* **example-shadow-api-detector:** use official published plugin in examples ([#96](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/96)) ([bca72d8](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/bca72d8e2dab7acecccb4cd75dafd95e030a43be)), closes [#96](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/96)


### BREAKING CHANGE

* **gherkin-to-asciidoc:** includeSubDirs and groupByFeature now default to
true (previously false). A project relying on the old defaults -
particularly one using sourceFile without explicitly setting
includeSubDirs = false, which will now fail validation - must set
includeSubDirs = false and/or groupByFeature = false explicitly to
keep its previous behaviour. Feature file processing order is also
now deterministic (alphabetical by path, directory files before
sub-directory files) instead of filesystem-dependent, which may
reorder scenarios in existing generated reports.

# [1.5.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/v1.4.0...v1.5.0) (2026-07-30)


### ✨ New and updated features

* **gherkin-to-asciidoc:** add a table of contents to generated reports ([#85](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/85)) ([ff1f465](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/ff1f465bf9b8cd42d3d5928fa966450091c72316)), closes [#85](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/85)

# [1.4.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/v1.3.0...v1.4.0) (2026-07-30)


### ✨ New and updated features

* **gherkin-to-asciidoc:** support Gradle multi-project builds ([#84](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/84)) ([2ba342c](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/2ba342c088c818f5ee615e7b353ae9f2c5fa6b74)), closes [#84](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/84)


### 🐛 Bug Fixes

* **CI:** stop the NVD cache refresh from timing out on every cold sync ([#83](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/83)) ([43b3710](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/43b37102fa064f3abd036782bad0f9bbef12c80b)), closes [#83](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/83)


### 🔧 Misc

* Change NVD cache refresh schedule to weekly ([#82](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/82)) ([834a2d5](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/834a2d5dfde09292efd754d32a5b58b35fd88e5d)), closes [#82](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/82)

# [1.3.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/v1.2.1...v1.3.0) (2026-07-27)


### ✨ New and updated features

* **gherkin-to-asciidoc:** report the system-under-test version ([#81](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/81)) ([87d6fdd](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/87d6fdd69337dd4a6fce12644e9262bacee388ac)), closes [#81](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/81)

## [1.2.1](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/v1.2.0...v1.2.1) (2026-07-27)


### 🔧 Misc

* Fix dependency compatibility and update package versions ([#80](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/80)) ([6123b6b](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/6123b6ba19399ea669404a9ae15dbc42a69d9fbc)), closes [#80](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/80)

# [1.2.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/v1.1.0...v1.2.0) (2026-07-24)


### ✨ New and updated features

* **gherkin-to-asciidoc:** write report snippets and support custom Mustache templates ([#79](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/79)) ([af9218b](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/af9218b776feb3716cfd1754c1493c308bd80ef3)), closes [#79](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/79)

# [1.1.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/v1.0.0...v1.1.0) (2026-07-24)


### ✨ New and updated features

* **gherkin-to-asciidoc:** group scenarios by feature in the generated report ([#78](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/78)) ([ea9d37d](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/ea9d37d481484c0e5a0eba732cb01a7c410dd62c)), closes [#78](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/78)

# [1.0.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/v0.8.0...v1.0.0) (2026-07-24)


### ✨ New and updated features

* **gherking-to-asciidoc:** update AsciiDoc generation with multi-directory support and explanations ([#77](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/77)) ([34502cd](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/34502cdf45e15c04435802d462cb9dbdba0ef501)), closes [#77](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/77)


### 🐛 Bug Fixes

* **gherkin-to-asciidoc:** stop CI test task failure in progress-tracking example ([#75](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/75)) ([c1bff23](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/c1bff2352a384b2f898f79e2ac20adfca10dc7e1)), closes [#75](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/75)


### 👷 CI/CD

* narrow plugin build workflow triggers to their own dependencies ([#76](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/76)) ([de157e9](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/de157e9a29305607eb8d730a3563f5233bba9ae7)), closes [#76](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/76) [#74](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/74)


### BREAKING CHANGE

* **gherking-to-asciidoc:** the sourceDir and glueCodeDir DSL properties have been
removed in favour of sourceDirs and glueCodeDirs. Existing
configuration such as `sourceDir = file('...')` must be changed to
`sourceDirs.from(file('...'))` (and likewise for glueCodeDir ->
glueCodeDirs).

* docs(gherkin-to-asciidoc): demonstrate multi-directory support in examples

Splits the progress-tracking example into two unrelated feature areas,
auth and billing, each with its own feature file directory and its own
glue code directory, wired up via sourceDirs.from(...) and
glueCodeDirs.from(...). Adds a fully-implemented InvoiceSteps/
invoice.feature pair for billing so the generated report shows a
scenario from a completely separate directory pair correctly counted
as implemented, proving the aggregation actually spans directories
rather than just documenting that it should.

Bumps both examples' pinned plugin version to 1.0.0 (the next version
given the breaking DSL rename in the previous commit). Verified against
a local publishToMavenLocal build before pinning, same as with the
trackProgress feature: won't build against the Gradle Plugin Portal
until 1.0.0 is actually released.

* feat(gherkin-to-asciidoc): explain report contents and status meanings in the output itself

The generated AsciiDoc previously jumped straight from the title into
bullet lists (or the progress table), with no explanation of what the
document contains - readers had to already know the plugin's
conventions, or go read the README, to understand it.

Plain mode now opens with a one-line description of what the document
lists. Progress-tracking mode additionally explains, in the document
itself, what listed/defined/implemented mean via a status legend table
right after the intro, and repeats the relevant one-line explanation
under each of the Listed/Defined/Implemented headings so a reader who
jumps straight to one section still gets the context without having to
scroll back up.

Verified end-to-end against both example projects via a temporary
publishToMavenLocal build, same as previous feature verifications.

# [0.8.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/v0.7.2...v0.8.0) (2026-07-24)


### ✨ New and updated features

* **gherkin-to-asciidoc:** Add scenario progress tracking and examples to gherkin-to-asciidoc ([#74](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/74)) ([0a9c220](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/0a9c2203009ab0c3041d1f387084569facec69ee)), closes [#74](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/74)

## [0.4.8](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/v0.4.7...v0.4.8) (2026-06-21)


### 🔧 Misc

* add documentation and workflows for the example projects ([#51](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/51)) ([b63be73](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/b63be7377a4592ecd57c99519f742d79ead49957)), closes [#51](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/51)
* dependency updates for Gradle plugin repository ([#52](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/52)) ([eed6f60](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/eed6f603c1abfaeb267a9982f8ff1b46d75fa1cd)), closes [#52](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/52)

## [0.4.5](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/v0.4.4...v0.4.5) (2026-06-12)


### 🔧 Misc

* dependency updates for Gradle plugin repository ([#45](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/45)) ([3da2bff](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/3da2bff82b31326e78cfa6973c546d1b81b0250b)), closes [#45](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/45)

# [0.2.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/v0.1.3...v0.2.0) (2026-04-30)


### ✨ New and updated features

* add gherkin-to-asciidoc plugin ([#18](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/18)) ([8ef5355](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/8ef535529fa3139f2ddb087f2f95b5de68421bd8)), closes [#18](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/18)


### 👷 CI/CD

* **security:** add weekly scheduled security scan wrapper workflow ([#17](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/17)) ([1ea9fdf](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/1ea9fdf9d6c29020dc0cfd9d90fe6eac06a06fb5)), closes [#17](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/17)

# Changelog
