## [0.4.2](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/api-only-suite-v0.4.1...api-only-suite-v0.4.2) (2026-08-13)


### 🐛 Bug Fixes

* **ci:** api-only-suite release workflow now fires on sibling-only releases ([#134](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/134)) ([8416a51](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/8416a51d1bce45f735f16e67024a7469f1b16aea)), closes [#134](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/134)
* renaming the history file path no longer leaves history-tracking tasks UP-TO-DATE ([#133](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/133)) ([9d7e7d6](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/9d7e7d653f207440acf61cd8fe6437707b48e7d2)), closes [#133](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/133)

## [0.4.1](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/api-only-suite-v0.4.0...api-only-suite-v0.4.1) (2026-08-13)


### 🐛 Bug Fixes

* **api-only-suite:** detectAllApiGaps no longer fails when an early detector finds a gap ([#132](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/132)) ([7fea3e7](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/7fea3e7a02508efb7d97bb7c899512b4636659eb)), closes [#132](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/132)

# [0.4.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/api-only-suite-v0.3.0...api-only-suite-v0.4.0) (2026-08-13)


### ✨ New and updated features

* track contract progress history and report it over time in all three API detector plugins ([#131](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/131)) ([e65f7bf](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/e65f7bf72557402f4078dcfa7abc0a10436103db)), closes [#131](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/131) [#59](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/59) [#59](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/59)


### 🐛 Bug Fixes

* **gherkin-to-asciidoc:** render Tracked since as a human-friendly timestamp ([#130](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/130)) ([96b50cd](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/96b50cd231a02e973be091e57d005373c52eb2be)), closes [#130](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/130)

# [0.3.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/api-only-suite-v0.2.0...api-only-suite-v0.3.0) (2026-08-13)


### ✨ New and updated features

* **gherkin-to-asciidoc:** log LIFECYCLE-level scan progress during feature docs generation ([#128](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/128)) ([1e269bd](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/1e269bdc496508e5083e69ee521fa1da1c26c7df)), closes [#128](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/128) [#56](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/56) [#56](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/56)


### 🐛 Bug Fixes

* **api-only-suite:** wait for sibling detector releases before resolving their published version ([#129](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/129)) ([db3d8c9](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/db3d8c9b86e48f522ee1864c3220b746af5f39fa)), closes [#129](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/129)

# [0.2.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/api-only-suite-v0.1.1...api-only-suite-v0.2.0) (2026-08-12)


### ✅ Tests

* **gherkin-to-asciidoc:** cover scenario renumbering when moved between features ([#125](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/125)) ([201a59e](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/201a59e5ff1ecd7996e28fd58529f72a17f0ff63)), closes [#125](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/125)


### ✨ New and updated features

* **examples:** add doppelganger-api-detector and api-only-suite examples ([#117](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/117)) ([f96de9e](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/f96de9eb9ec6115c73a3ac91a2e71baa0f61caab)), closes [#117](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/117)
* **examples:** add mirage-api-detector scan-mocks example ([#124](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/124)) ([edc641d](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/edc641d99667c65fa9355008b215effae8988450)), closes [#124](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/124)
* **mirage-api-detector:** add scanMocks/stubDirs to detect mirage APIs from WireMock stubs ([#123](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/123)) ([3aa5958](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/3aa5958efc35d14af0cdd2d1d8579d20a4d115fe)), closes [#123](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/123)
* **gherkin-to-asciidoc:** add trackProgressHistory to persist per-scenario progress history ([#126](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/126)) ([f4212f1](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/f4212f1d47f4036270d953fdaf6bd27642cb2bb4)), closes [#126](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/126) [#114](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/114)
* log LIFECYCLE-level scan progress in all three API detector plugins ([#127](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/127)) ([cb3dcfb](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/cb3dcfb49d08092d8e7fc419a1c71453ee47f82a)), closes [#127](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/127) [#56](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/56) [#56](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/56)


### 🐛 Bug Fixes

* **doppelganger-api-detector:** default testDirs/contractsDir to a dedicated testContract source set ([#121](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/121)) ([f3b81e9](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/f3b81e94dae76ee42a25d00b8a89bd8810e544fa)), closes [#121](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/121)
* **examples:** explicitly opt in to the doppelganger verification sources these examples demonstrate ([#120](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/120)) ([0810b76](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/0810b760883dc7ec596c4df3613b5518e7b5da60)), closes [#120](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/120)
* **doppelganger-api-detector:** only enable Spring RestDocs by default ([#119](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/119)) ([ffd0336](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/ffd03368b4f89b11e2b3cb064a018853b2781b9a)), closes [#119](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/119)
* **examples:** point doppelganger examples at their actual src/test source location ([#122](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/122)) ([eb8e8f4](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/eb8e8f46f7e3c2ce41b5388e43f00b51d445b4f0)), closes [#122](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/122)
* **api-only-suite:** publish sibling plugin versions instead of the placeholder ([#116](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/116)) ([5506967](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/5506967f684b8a85112191e2d7070ac5ce9b2cb0)), closes [#116](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/116)
* **api-only-suite:** scope the Gradle Portal publish to the root project only ([#115](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/115)) ([a2fb3ea](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/a2fb3ea9444c2d4185e10721fdfeee79696d484b)), closes [#115](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/115)
* **docs:** update repo root index and per-plugin GitHub release badges ([#118](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/118)) ([9385925](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/9385925815598e99316379bcbd4d31e81f534f10)), closes [#118](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/118)

## [0.1.1](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/api-only-suite-v0.1.0...api-only-suite-v0.1.1) (2026-08-12)


### 🐛 Bug Fixes

* **doppelganger-api-detector:** allow manually dispatching the release workflow ([#113](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/113)) ([714b739](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/714b739a79875cf238a1833d79eee15b648563c9)), closes [#113](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/113)
* depend on the published api-detector-core artifact ([#114](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/114)) ([13313e6](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/13313e683f0519f446c82f0333b7030024a468b8)), closes [#114](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/114)

# [0.1.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/api-only-suite-v0.0.0...api-only-suite-v0.1.0) (2026-08-12)


### ✨ New and updated features

* add Doppelganger API Detector and Arc-E-Tect API-Only Suite plugins ([#111](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/111)) ([f59ee4d](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/f59ee4dba895f13ad8f49fb3de893c4304bd2645)), closes [#111](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/111)
