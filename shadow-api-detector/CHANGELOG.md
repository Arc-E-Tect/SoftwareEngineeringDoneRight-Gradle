# [1.0.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/shadow-api-detector-v0.4.0...shadow-api-detector-v1.0.0) (2026-08-10)


### ✨ New and updated features

* **gherkin-to-asciidoc:** add ci indexing value and a CLI override for the whole build ([#102](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/102)) ([063c069](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/063c069e406f4b2ffe7f40016a4af6ceb45d2999)), closes [#102](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/102) [#99](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/99)
* **example-shadow-api-detector:** add composed RequestMapping example ([#98](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/98)) ([6ea5163](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/6ea5163e61c1fd8d271b356e0bbdff6e48f459c1)), closes [#98](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/98)
* **gherkin-to-asciidoc:** add forceRewrite to skip renumbering already-numbered lines ([#103](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/103)) ([48a74de](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/48a74decfe2c73dfdefca747a65922756820984a)), closes [#103](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/103)
* **gherkin-to-asciidoc:** add indexing DSL property to number features and scenarios ([#99](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/99)) ([5c207c4](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/5c207c4e0052499c6b796a3b7640a7b13e7b676f)), closes [#99](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/99)
* **shadow-api-detector:** stop auto-wiring detectShadowApis into check ([#105](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/105)) ([03a3da7](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/03a3da700c913d2ff743698c09c44c476b93a958)), closes [#105](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/105)


### 🐛 Bug Fixes

* **example-shadow-api-detector:** use official published plugin in examples ([#96](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/96)) ([bca72d8](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/bca72d8e2dab7acecccb4cd75dafd95e030a43be)), closes [#96](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/96)


### 📝 Documentation

* **gherkin-to-asciidoc:** add multi-project example demonstrating the indexing modes ([#100](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/100)) ([a0cd7a7](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/a0cd7a7c481fdd11079b77199a16c4b43c8b1104)), closes [#100](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/100)
* **gherkin-to-asciidoc:** document the indexing property and the includeSubDirs/groupByFeature default changes ([#101](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/101)) ([9fe4afd](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/9fe4afd23b5ee85ab95f322e4115cc67129dd2ac)), closes [#101](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/101) [#99](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/99)


### BREAKING CHANGE

* **shadow-api-detector:** detectShadowApis no longer runs automatically as part
of check/build. Projects relying on the previous automatic wiring must
add the dependsOn shown above to keep the check in their build.
* **gherkin-to-asciidoc:** indexing's default numbering behaviour changes.
Previously every generateFeatureDocs run fully renumbered every
Feature/Scenario from scratch; by default it now preserves numbers
that already match the current indexing value's format instead. Set
forceRewrite = true (or -PgherkinToAsciidoc.forceRewrite=true) to
keep the old always-renumber-everything behaviour.
* **gherkin-to-asciidoc:** includeSubDirs and groupByFeature now default to
true (previously false). A project relying on the old defaults -
particularly one using sourceFile without explicitly setting
includeSubDirs = false, which will now fail validation - must set
includeSubDirs = false and/or groupByFeature = false explicitly to
keep its previous behaviour. Feature file processing order is also
now deterministic (alphabetical by path, directory files before
sub-directory files) instead of filesystem-dependent, which may
reorder scenarios in existing generated reports.

# [0.4.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/shadow-api-detector-v0.3.0...shadow-api-detector-v0.4.0) (2026-08-03)


### ✨ New and updated features

* **shadow-api-detector:** add OpenAPI 3.2 compatibility support and example ([#95](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/95)) ([c57432b](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/c57432b4fc73fc9043291edef634ea7ba22d89e7)), closes [#95](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/95)


### 🐛 Bug Fixes

* **gherkin-to-asciidoc:** set systemUnderTestVersion in snippet-templates example ([#94](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/94)) ([ca7fb34](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/ca7fb346bd41113294928ab441ba0099fcf4301a)), closes [#94](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/94)


### 🔧 Misc

* consolidate dependency vulnerability scanning into the NVD cache workflow ([#92](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/92)) ([35f953a](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/35f953a0b7768be3a70b9cd53213d7d7d24f3d34)), closes [#92](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/92)
* dependency updates for Gradle plugin repository ([#93](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/93)) ([fe4ca4f](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/fe4ca4f9599058f3bcb9a15f466aaf17d27162a1)), closes [#93](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/93)

# [0.3.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/shadow-api-detector-v0.2.0...shadow-api-detector-v0.3.0) (2026-07-31)


### ✨ New and updated features

* **shadow-api-detector:** include system under test version in the report ([#91](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/91)) ([bb68915](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/bb68915d986be2abd81ccf6e4c196c2f60692e6b)), closes [#91](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/91)

# [0.2.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/shadow-api-detector-v0.1.0...shadow-api-detector-v0.2.0) (2026-07-31)


### ✨ New and updated features

* **shadow-api-detector:** add a "what is a shadow API" preamble to the report ([#90](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/90)) ([19cfd12](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/19cfd122ebcd31a314479a216719ed821a465f92)), closes [#90](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/90)


### 📝 Documentation

* **shadow-api-detector:** add example projects for the published plugin ([#89](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/89)) ([db993c3](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/db993c30bd318d02d41d7c2cef2fe26055826652)), closes [#89](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/89)

# [0.1.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/shadow-api-detector-v0.0.0...shadow-api-detector-v0.1.0) (2026-07-31)


### ✨ New and updated features

* **zombie-api-detector:** add zombie-api-detector Gradle plugin ([#86](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/86)) ([dc57220](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/dc5722082b0f6e724d714bbfe7eb4849d562b85e)), closes [#86](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/86)


### 🐛 Bug Fixes

* **CI:** make the existing-tag check respect a project's tagFormat ([#87](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/87)) ([94a076a](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/94a076aefb6b219413cad3f43c5530ce624408b8)), closes [#87](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/87)


### rename

* **shadow-api-detector:** rename zombie-api-detector to shadow-api-detector ([#88](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/88)) ([832967e](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/832967e4f8d99bbfb240cd459ab79b360a707838)), closes [#88](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/88)
