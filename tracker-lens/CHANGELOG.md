# [2.0.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/tracker-lens-v1.5.1...tracker-lens-v2.0.0) (2026-08-18)


### ⏪ Reverts

* move jacoco-exclusion-report and gherkin-to-asciidoc semver scoping out of main ([#184](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/184)) ([31aaae5](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/31aaae5c55e35d1e10998d1e92cf603b5c4d866c)), closes [#184](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/184)


### ✨ New and updated features

* **shadow-api-detector:** add --scanForShadows to scan a single controller from the CLI ([#183](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/183)) ([e4f0e6d](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/e4f0e6da3fdadfa77fd0637bcfb230ddc64ac833)), closes [#183](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/183)
* **mirage-api-detector:** scan controllers and stubs together, not exclusively ([#181](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/181)) ([85db5b5](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/85db5b5c9c22757c76944ba3288e6d222418c6da)), closes [#181](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/181)


### 🐛 Bug Fixes

* add cross-plugin edge-case coverage ([#187](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/187)) ([71480ff](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/71480ffa0def70aa1e4ca8c39f87a21c609c1ce6)), closes [#187](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/187)
* catalog example Gradle dependencies ([#186](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/186)) ([5a3994c](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/5a3994c355033bffc4f78475f8d2d38bd70187da)), closes [#186](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/186)
* **api-only-suite:** forward basePath to mirageApiGapsForSuite ([#182](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/182)) ([ffcc8af](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/ffcc8af1740b333baf64ecd612470c50a054a109)), closes [#182](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/182) [#177](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/177)


### 🔧 Misc

* dependency updates for Gradle plugin repository ([#188](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/188)) ([a650e7c](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/a650e7c01e4c092434230f18ebb8e5b6e7e197b9)), closes [#188](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/188)


### Breaking change

* **mirage-api-detector:** a project relying on scanMocks = true to mean "check against stubs instead of
controllers" now also gets real controller-based mirage detection in the same run, and its
contractHistoryFile can gain implementedAt entries it previously never would have. Verified
end-to-end against a real project via a scratch mavenLocal publish: contract history now
correctly carries both implementedAt and stubbedAt for endpoints with both kinds of evidence in
the same run, where previously that was structurally impossible.

### BREAKING CHANGE

* **shadow-api-detector:** the -PshadowApiDetector.updateContractHistory=<value>
project property no longer has any effect. Use
detectShadowApis --updateContractHistory / --no-updateContractHistory
instead.

## [1.5.1](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/tracker-lens-v1.5.0...tracker-lens-v1.5.1) (2026-08-17)


### 🐛 Bug Fixes

* **api-only-suite:** bump api-detector-core to 1.2.0 ([#179](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/179)) ([9c62c91](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/9c62c91dee506998240521ea85c609c2874c8e0f)), closes [#179](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/179) [#177](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/177)
* **mirage-api-detector:** strip a base path from stub-scanned paths before matching ([#177](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/177)) ([f276bef](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/f276beffa57b09bbdd4165ac97417127bf47e14c)), closes [#177](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/177)
* **gherkin-to-asciidoc:** strip numbering from persisted history fields ([#178](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/178)) ([c3b80cb](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/c3b80cb22e9b3f7c130fc3f1e0c8937f9b5fb15e)), closes [#178](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/178)
* **tracker-lens:** zero-fill chart values past a tracker's last real data point ([#180](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/180)) ([1f2fb61](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/1f2fb617b38e83abe1edab5a509951a205fc284f)), closes [#180](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/180) [#dashboard-data](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/dashboard-data)

# [1.5.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/tracker-lens-v1.4.0...tracker-lens-v1.5.0) (2026-08-17)


### ✨ New and updated features

* **examples:** generate the custom-dashboard-template example's fixture instead of committing it ([#172](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/172)) ([02900b3](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/02900b39c64a07145b3466d7d67c15a711e49cbc)), closes [#172](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/172)
* **examples:** generate the custom-lens tracker-lens example's fixture instead of committing it ([#171](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/171)) ([1fe2227](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/1fe22276405c2fb0917e30efb62eb004e4837731)), closes [#171](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/171)
* **examples:** generate the external-lens-pack example's fixture instead of committing it ([#174](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/174)) ([f3c4e59](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/f3c4e59f19d093eb40b0853ae6bb5972468bd2a9)), closes [#174](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/174)
* **examples:** generate the high-contrast-accessibility example's fixture instead of committing it ([#173](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/173)) ([36ecd86](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/36ecd86739a5c79264652269d05273dcc1a05a28)), closes [hi#contrast-accessibility](https://github.com/hi/issues/contrast-accessibility) [#173](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/173) [hi#contrast-lens](https://github.com/hi/issues/contrast-lens)
* **examples:** generate the plain tracker-lens example's fixture instead of committing it ([#170](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/170)) ([7c081d8](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/7c081d82cda19794c8ad3202a5cd573022631cb5)), closes [#170](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/170)


### 🐛 Bug Fixes

* **tracker-lens:** start the chart on the earliest data point, not a fixed 30-day lookback ([#176](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/176)) ([d8c3fe3](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/d8c3fe3a1bc40c6da4c1ca2c9256d7cd62070aa4)), closes [#176](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/176)


### 📝 Documentation

* **api-only-suite:** add microservices multi-project example ([#175](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/175)) ([52f7913](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/52f7913e3dded1e9401d8b0907e47a008d9c0a83)), closes [#175](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/175)
* **examples:** stop freezing scaffolded-lens-pack's generated output ([#169](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/169)) ([4203c22](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/4203c2256ea87851ec4470db748efc60877f64fa)), closes [#169](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/169) [tracker-lens#168](https://github.com/tracker-lens/issues/168)

# [1.4.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/tracker-lens-v1.3.1...tracker-lens-v1.4.0) (2026-08-16)


### ✨ New and updated features

* **tracker-lens:** make bootstrapTrackerLensProject use the fixture generator ([#168](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/168)) ([f97bc52](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/f97bc5210ed57ce12871b851fb8f71f52f74e055)), closes [#168](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/168)

## [1.3.1](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/tracker-lens-v1.3.0...tracker-lens-v1.3.1) (2026-08-16)


### 🐛 Bug Fixes

* **tracker-lens:** make generateTrackerLensFixture always re-execute ([#167](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/167)) ([db73cb8](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/db73cb85b705cfc39941941bf197c24718d40c86)), closes [#167](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/167)

# [1.3.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/tracker-lens-v1.2.0...tracker-lens-v1.3.0) (2026-08-16)


### ✨ New and updated features

* **tracker-lens:** add a calibrated fixture generator for tracker-lens dashboards ([#166](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/166)) ([ce0eaa6](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/ce0eaa6a544a64df79bcaf6721e184438ea7c717)), closes [#166](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/166)


### 🐛 Bug Fixes

* **docs:** mention the schema-version marker in the detector plugins' NDJSON examples ([#165](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/165)) ([0ba9880](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/0ba98809c9b57b06e25a5f68770c34014c06c10b)), closes [#165](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/165)

# [1.2.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/tracker-lens-v1.1.0...tracker-lens-v1.2.0) (2026-08-16)


### ✨ New and updated features

* **tracker-lens:** add a favicon to the bundled default template ([#164](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/164)) ([352c833](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/352c833549feece00a04c0ba87e60d7e34748295)), closes [#164](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/164)
* **gherkin-to-asciidoc:** write and tolerate a schema-version marker ([#163](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/163)) ([10fcb7b](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/10fcb7bf3ec854eebc80db670e1f1dea32e60eaa)), closes [#163](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/163)

# [1.1.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/tracker-lens-v1.0.0...tracker-lens-v1.1.0) (2026-08-16)


### ✨ New and updated features

* **tracker-lens:** add per-date stage breakdown to #dashboard-data ([#162](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/162)) ([dd80c04](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/dd80c0414e1937298098fcc1e571231dfb932942)), closes [#dashboard-data](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/dashboard-data) [#162](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/162)


### 🐛 Bug Fixes

* **api-only-suite:** bump api-detector-core to 1.0.0 ([#161](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/161)) ([85f7eec](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/85f7eec397dac228e4f5d0083ab27742ff8a3593)), closes [#161](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/161) [158/#159](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/159)

# [1.0.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/tracker-lens-v0.5.0...tracker-lens-v1.0.0) (2026-08-15)


### ✨ New and updated features

* **shadow-api-detector:** adapt to api-detector-core's narrower implementedAt semantics ([#158](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/158)) ([ab1c5e7](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/ab1c5e7a8cdb4572f5e3b1684262937078556d23)), closes [#158](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/158)
* **doppelganger-api-detector:** adapt to api-detector-core's narrower implementedAt semantics ([#159](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/159)) ([d83230f](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/d83230f5cd11ea2455ba29f01dbb5c90623758cb)), closes [#159](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/159)
* **tracker-lens:** add stubbed stage for the API_CONTRACT tracker source ([#160](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/160)) ([57a45b5](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/57a45b50e81b16b4bb40303cc88b25464857348a)), closes [#160](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/160) [light/dark/hi#contrast](https://github.com/light/dark/hi/issues/contrast) [hi#contrast](https://github.com/hi/issues/contrast)
* **mirage-api-detector:** separate real implementation evidence from stub evidence in contract history ([#157](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/157)) ([d62db26](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/d62db26abbdd96841ea410802953c9e33d168371)), closes [#157](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/157)


### 🐛 Bug Fixes

* **tracker-lens:** serialize release workflow runs to stop version races ([#156](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/156)) ([c4d9e79](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/c4d9e798b56ae06077dc6a2429f06eae9d08af44)), closes [#156](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/156)


### BREAKING CHANGE

* **tracker-lens:** a contractHistoryFile written by a pre-stubbedAt version
of the API detector plugins (9 fields) no longer matches
ApiContractTrackerSource's parser at all and is now skipped as malformed
line-by-line, same as any other unparseable line - producing an empty
tracker section rather than a populated one. Consumers should upgrade
their contractHistoryFile (see mirage-api-detector's migrateContractHistory
task) before regenerating a dashboard against it. A custom lens that
doesn't yet define a 4th --dashboard-stage-N color will fall back to
reusing stage-1's color for the new stubbed series.
* **doppelganger-api-detector:** an existing contractHistoryFile (9 fields, no stubbedAt)
now fails detectDoppelgangerApis with a clear error instead of loading.
Apply mirage-api-detector and run its migrateContractHistory task once to
upgrade the file in place, or point contractHistoryFile at a new location
to start fresh.
* **shadow-api-detector:** an existing contractHistoryFile (9 fields, no stubbedAt)
now fails detectShadowApis with a clear error instead of loading. Apply
mirage-api-detector and run its migrateContractHistory task once to
upgrade the file in place, or point contractHistoryFile at a new location
to start fresh.
* **mirage-api-detector:** an existing contractHistoryFile (9 fields, no stubbedAt)
now fails detectMirageApis with a clear error instead of loading. Run
migrateContractHistory once to upgrade it in place, or point
contractHistoryFile at a new location to start fresh.

# [0.5.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/tracker-lens-v0.4.0...tracker-lens-v0.5.0) (2026-08-15)


### ✨ New and updated features

* **tracker-lens:** let lens packs ship selectable dashboard templates ([#155](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/155)) ([fa06b2a](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/fa06b2a5838c65fc0b675cb7e949d3f76c851fe0)), closes [#155](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/155)

# [0.4.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/tracker-lens-v0.3.2...tracker-lens-v0.4.0) (2026-08-15)


### ✨ New and updated features

* **dashboard-extra-charts:** full-width forecast chart and lens-colored ([#153](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/153)) ([7c86d33](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/7c86d33594f9b7961081acf6793633c26b9d158a)), closes [#153](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/153)
* **dashboard-extra-charts:** make lens-colored a real, integrated lens-pack lens ([#154](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/154)) ([6e4e5f2](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/6e4e5f280f7e651076fa71d1f6a38fe6ea2c03e5)), closes [#154](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/154) [#151](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/151) [#152](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/152)

## [0.3.2](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/tracker-lens-v0.3.1...tracker-lens-v0.3.2) (2026-08-15)


### 🐛 Bug Fixes

* **tracker-lens:** make every tracker's line chart fill its container width ([#152](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/152)) ([202d2ca](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/202d2ca2a08f07a3fc1cddc2db1899b066524c4e)), closes [#152](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/152)
* **tracker-lens:** make projected-completion dates workday-aware ([#151](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/151)) ([086aecb](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/086aecb5bd145bb1f8e8434a62522008c42ca09c)), closes [#151](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/151)


### 🔧 Misc

* **examples:** bump tracker-lens to 0.3.1 across examples ([#150](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/150)) ([cce801a](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/cce801a5bdecffae223e88450a34fa9cb26569e1)), closes [#150](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/150)

## [0.3.1](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/tracker-lens-v0.3.0...tracker-lens-v0.3.1) (2026-08-15)


### 🐛 Bug Fixes

* **tracker-lens:** use stageBreakdown for GHERKIN_SCENARIO metric cards ([#149](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/149)) ([6d7c99b](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/6d7c99b5ee21cf879f747184aed58c2572572221)), closes [#149](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/149)

# [0.3.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/tracker-lens-v0.2.0...tracker-lens-v0.3.0) (2026-08-14)


### ✨ New and updated features

* **tracker-lens:** add dashboardName and version DSL properties ([#148](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/148)) ([8c0083a](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/8c0083a6caaa99f15f682cadb55a05cddfcb8a7f)), closes [#148](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/148)


### 🐛 Bug Fixes

* **tracker-lens:** correct dashboard-extra-charts' Gherkin pie and api-contracts numbers ([#145](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/145)) ([39a3bcc](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/39a3bcc182202fd740002a536dc1aa6e38d82ceb)), closes [#145](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/145) [#2f9e44](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/2f9e44) [#7048e8](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/7048e8)
* **tracker-lens:** rewrite bdd-scenarios' metric cards to match its own pie ([#147](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/147)) ([03a9c95](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/03a9c9506f0765e3a535991a61afa37a4c428d75)), closes [#147](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/147)

# [0.2.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/tracker-lens-v0.1.0...tracker-lens-v0.2.0) (2026-08-14)


### ✨ New and updated features

* **tracker-lens:** expose per-item current-stage breakdown in #dashboard-data ([#146](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/146)) ([c38c4f3](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/c38c4f311498fcd5a5e2ee5ef531ea17d49e07c1)), closes [#dashboard-data](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/dashboard-data) [#146](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/146) [#dashboard-data](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/dashboard-data)


### 🐛 Bug Fixes

* **docs:** correct include directives in README for user authentication and invoice payment sections ([#144](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/144)) ([1afecc8](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/1afecc85e3f773b4380a63dc0b731e396667e12c)), closes [#144](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/144)
* **doppelganger-api-detector:** recognize spring-restdocs-restassured and strip OpenAPI server base path ([#140](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/140)) ([b71ef3b](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/b71ef3b00ad81447a0b8246b27c2d503c0fc8fd0)), closes [#140](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/140)


### 📝 Documentation

* **tracker-lens:** add dashboard-extra-charts example (pie chart + nested Venn diagram) ([#143](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/143)) ([91423f3](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/91423f36fe57039b4c92081d3f955e6bfa87841e)), closes [#143](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/143) [#dashboard-data](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/dashboard-data)
* **tracker-lens:** add eight runnable examples and fix example TOC nesting ([#139](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/139)) ([92f6a2d](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/92f6a2d9fc85fa94bc0c5e9cab11a814c0db08d9)), closes [#139](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/139)
* **doppelganger-api-detector:** add verified-by-restdocs-restassured example ([#141](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/141)) ([0e88269](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/0e882691e014552337d163a0acfeed7c811b84bb)), closes [#141](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/141) [#140](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/140) [#140](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/140)
* **tracker-lens:** add Vulnerability Scan, Gradle Plugin Portal, and GitHub release badges ([#142](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/142)) ([8c0a602](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/8c0a602c2abe4eb4dbec0cfca98336563ca50f37)), closes [#142](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/142)

# [0.1.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/tracker-lens-v0.0.1...tracker-lens-v0.1.0) (2026-08-14)


### ✨ New and updated features

* **tracker-lens:** add initTrackerLens and bootstrapTrackerLensProject scaffolding tasks ([#138](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/138)) ([60fc170](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/60fc1707ce066bb26b112d74ee658fdcab5d9592)), closes [#138](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/138)

## [0.0.1](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/tracker-lens-v0.0.0...tracker-lens-v0.0.1) (2026-08-13)

<!-- This file is managed by semantic-release. Entries are generated automatically from conventional commits at release time. -->
