## [3.0.2](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/shadow-api-detector-v3.0.1...shadow-api-detector-v3.0.2) (2026-08-20)


### 🐛 Bug Fixes

* **shadow-api-detector:** don't fail the build on a missing rootDocument or controllerDirs ([#198](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/198)) ([534aad9](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/534aad950b43dd7bb3eb5ca4de73b41ef9cbd8d2)), closes [#198](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/198)
* **example-mirage-scan-mocks:** restore expected-failure behavior for CI gate ([#189](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/189)) ([ddbbede](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/ddbbedeccb4f144d0459352a0d9f734a8a5bf8e5)), closes [#189](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/189)
* **gherkin-to-asciidoc:** support And/But glue and full Gherkin step keywords ([#197](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/197)) ([7b88725](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/7b88725d1d606288301e31a0aed56a41bb34ba60)), closes [#197](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/197)


### 📝 Documentation

* **readme:** add plugin versions to each plugin ([e2fae93](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/e2fae933ce4901431dec2f570a3c8473b3357702))

## [3.0.1](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/shadow-api-detector-v3.0.0...shadow-api-detector-v3.0.1) (2026-08-18)


### ⏪ Reverts

* move jacoco-exclusion-report and gherkin-to-asciidoc semver scoping out of main ([#184](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/184)) ([31aaae5](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/31aaae5c55e35d1e10998d1e92cf603b5c4d866c)), closes [#184](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/184)


### 🐛 Bug Fixes

* add cross-plugin edge-case coverage ([#187](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/187)) ([71480ff](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/71480ffa0def70aa1e4ca8c39f87a21c609c1ce6)), closes [#187](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/187)
* catalog example Gradle dependencies ([#186](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/186)) ([5a3994c](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/5a3994c355033bffc4f78475f8d2d38bd70187da)), closes [#186](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/186)


### 🔧 Misc

* dependency updates for Gradle plugin repository ([#188](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/188)) ([a650e7c](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/a650e7c01e4c092434230f18ebb8e5b6e7e197b9)), closes [#188](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/188)

# [3.0.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/shadow-api-detector-v2.0.0...shadow-api-detector-v3.0.0) (2026-08-17)


### ✨ New and updated features

* **doppelganger-api-detector:** adapt to api-detector-core's narrower implementedAt semantics ([#159](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/159)) ([d83230f](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/d83230f5cd11ea2455ba29f01dbb5c90623758cb)), closes [#159](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/159)
* **shadow-api-detector:** add --scanForShadows to scan a single controller from the CLI ([#183](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/183)) ([e4f0e6d](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/e4f0e6da3fdadfa77fd0637bcfb230ddc64ac833)), closes [#183](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/183)
* **tracker-lens:** add a calibrated fixture generator for tracker-lens dashboards ([#166](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/166)) ([ce0eaa6](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/ce0eaa6a544a64df79bcaf6721e184438ea7c717)), closes [#166](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/166)
* **tracker-lens:** add a favicon to the bundled default template ([#164](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/164)) ([352c833](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/352c833549feece00a04c0ba87e60d7e34748295)), closes [#164](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/164)
* **tracker-lens:** add per-date stage breakdown to #dashboard-data ([#162](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/162)) ([dd80c04](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/dd80c0414e1937298098fcc1e571231dfb932942)), closes [#dashboard-data](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/dashboard-data) [#162](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/162)
* **tracker-lens:** add stubbed stage for the API_CONTRACT tracker source ([#160](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/160)) ([57a45b5](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/57a45b50e81b16b4bb40303cc88b25464857348a)), closes [#160](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/160) [light/dark/hi#contrast](https://github.com/light/dark/hi/issues/contrast) [hi#contrast](https://github.com/hi/issues/contrast)
* **examples:** generate the custom-dashboard-template example's fixture instead of committing it ([#172](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/172)) ([02900b3](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/02900b39c64a07145b3466d7d67c15a711e49cbc)), closes [#172](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/172)
* **examples:** generate the custom-lens tracker-lens example's fixture instead of committing it ([#171](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/171)) ([1fe2227](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/1fe22276405c2fb0917e30efb62eb004e4837731)), closes [#171](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/171)
* **examples:** generate the external-lens-pack example's fixture instead of committing it ([#174](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/174)) ([f3c4e59](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/f3c4e59f19d093eb40b0853ae6bb5972468bd2a9)), closes [#174](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/174)
* **examples:** generate the high-contrast-accessibility example's fixture instead of committing it ([#173](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/173)) ([36ecd86](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/36ecd86739a5c79264652269d05273dcc1a05a28)), closes [hi#contrast-accessibility](https://github.com/hi/issues/contrast-accessibility) [#173](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/173) [hi#contrast-lens](https://github.com/hi/issues/contrast-lens)
* **examples:** generate the plain tracker-lens example's fixture instead of committing it ([#170](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/170)) ([7c081d8](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/7c081d82cda19794c8ad3202a5cd573022631cb5)), closes [#170](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/170)
* **tracker-lens:** make bootstrapTrackerLensProject use the fixture generator ([#168](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/168)) ([f97bc52](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/f97bc5210ed57ce12871b851fb8f71f52f74e055)), closes [#168](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/168)
* **mirage-api-detector:** scan controllers and stubs together, not exclusively ([#181](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/181)) ([85db5b5](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/85db5b5c9c22757c76944ba3288e6d222418c6da)), closes [#181](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/181)
* **gherkin-to-asciidoc:** write and tolerate a schema-version marker ([#163](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/163)) ([10fcb7b](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/10fcb7bf3ec854eebc80db670e1f1dea32e60eaa)), closes [#163](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/163)


### 🐛 Bug Fixes

* **api-only-suite:** bump api-detector-core to 1.0.0 ([#161](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/161)) ([85f7eec](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/85f7eec397dac228e4f5d0083ab27742ff8a3593)), closes [#161](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/161) [158/#159](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/159)
* **api-only-suite:** bump api-detector-core to 1.2.0 ([#179](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/179)) ([9c62c91](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/9c62c91dee506998240521ea85c609c2874c8e0f)), closes [#179](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/179) [#177](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/177)
* **api-only-suite:** forward basePath to mirageApiGapsForSuite ([#182](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/182)) ([ffcc8af](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/ffcc8af1740b333baf64ecd612470c50a054a109)), closes [#182](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/182) [#177](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/177)
* **tracker-lens:** make generateTrackerLensFixture always re-execute ([#167](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/167)) ([db73cb8](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/db73cb85b705cfc39941941bf197c24718d40c86)), closes [#167](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/167)
* **docs:** mention the schema-version marker in the detector plugins' NDJSON examples ([#165](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/165)) ([0ba9880](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/0ba98809c9b57b06e25a5f68770c34014c06c10b)), closes [#165](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/165)
* **tracker-lens:** start the chart on the earliest data point, not a fixed 30-day lookback ([#176](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/176)) ([d8c3fe3](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/d8c3fe3a1bc40c6da4c1ca2c9256d7cd62070aa4)), closes [#176](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/176)
* **mirage-api-detector:** strip a base path from stub-scanned paths before matching ([#177](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/177)) ([f276bef](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/f276beffa57b09bbdd4165ac97417127bf47e14c)), closes [#177](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/177)
* **gherkin-to-asciidoc:** strip numbering from persisted history fields ([#178](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/178)) ([c3b80cb](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/c3b80cb22e9b3f7c130fc3f1e0c8937f9b5fb15e)), closes [#178](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/178)
* **tracker-lens:** zero-fill chart values past a tracker's last real data point ([#180](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/180)) ([1f2fb61](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/1f2fb617b38e83abe1edab5a509951a205fc284f)), closes [#180](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/180) [#dashboard-data](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/dashboard-data)


### 📝 Documentation

* **api-only-suite:** add microservices multi-project example ([#175](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/175)) ([52f7913](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/52f7913e3dded1e9401d8b0907e47a008d9c0a83)), closes [#175](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/175)
* **examples:** stop freezing scaffolded-lens-pack's generated output ([#169](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/169)) ([4203c22](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/4203c2256ea87851ec4470db748efc60877f64fa)), closes [#169](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/169) [tracker-lens#168](https://github.com/tracker-lens/issues/168)


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

# [2.0.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/shadow-api-detector-v1.4.2...shadow-api-detector-v2.0.0) (2026-08-15)


### ✨ New and updated features

* **shadow-api-detector:** adapt to api-detector-core's narrower implementedAt semantics ([#158](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/158)) ([ab1c5e7](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/ab1c5e7a8cdb4572f5e3b1684262937078556d23)), closes [#158](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/158)
* **tracker-lens:** add dashboardName and version DSL properties ([#148](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/148)) ([8c0083a](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/8c0083a6caaa99f15f682cadb55a05cddfcb8a7f)), closes [#148](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/148)
* **tracker-lens:** add initTrackerLens and bootstrapTrackerLensProject scaffolding tasks ([#138](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/138)) ([60fc170](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/60fc1707ce066bb26b112d74ee658fdcab5d9592)), closes [#138](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/138)
* **tracker-lens:** add Tracker Lens dashboard plugin ([#137](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/137)) ([3389c79](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/3389c79ad7f35e8d7f80f9da701d8fd77df63224)), closes [#137](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/137)
* **tracker-lens:** expose per-item current-stage breakdown in #dashboard-data ([#146](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/146)) ([c38c4f3](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/c38c4f311498fcd5a5e2ee5ef531ea17d49e07c1)), closes [#dashboard-data](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/dashboard-data) [#146](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/146) [#dashboard-data](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/dashboard-data)
* **dashboard-extra-charts:** full-width forecast chart and lens-colored ([#153](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/153)) ([7c86d33](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/7c86d33594f9b7961081acf6793633c26b9d158a)), closes [#153](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/153)
* **tracker-lens:** let lens packs ship selectable dashboard templates ([#155](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/155)) ([fa06b2a](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/fa06b2a5838c65fc0b675cb7e949d3f76c851fe0)), closes [#155](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/155)
* **dashboard-extra-charts:** make lens-colored a real, integrated lens-pack lens ([#154](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/154)) ([6e4e5f2](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/6e4e5f280f7e651076fa71d1f6a38fe6ea2c03e5)), closes [#154](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/154) [#151](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/151) [#152](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/152)
* **mirage-api-detector:** separate real implementation evidence from stub evidence in contract history ([#157](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/157)) ([d62db26](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/d62db26abbdd96841ea410802953c9e33d168371)), closes [#157](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/157)


### 🐛 Bug Fixes

* **tracker-lens:** correct dashboard-extra-charts' Gherkin pie and api-contracts numbers ([#145](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/145)) ([39a3bcc](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/39a3bcc182202fd740002a536dc1aa6e38d82ceb)), closes [#145](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/145) [#2f9e44](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/2f9e44) [#7048e8](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/7048e8)
* **docs:** correct include directives in README for user authentication and invoice payment sections ([#144](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/144)) ([1afecc8](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/1afecc85e3f773b4380a63dc0b731e396667e12c)), closes [#144](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/144)
* **tracker-lens:** make every tracker's line chart fill its container width ([#152](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/152)) ([202d2ca](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/202d2ca2a08f07a3fc1cddc2db1899b066524c4e)), closes [#152](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/152)
* **tracker-lens:** make projected-completion dates workday-aware ([#151](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/151)) ([086aecb](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/086aecb5bd145bb1f8e8434a62522008c42ca09c)), closes [#151](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/151)
* **doppelganger-api-detector:** recognize spring-restdocs-restassured and strip OpenAPI server base path ([#140](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/140)) ([b71ef3b](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/b71ef3b00ad81447a0b8246b27c2d503c0fc8fd0)), closes [#140](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/140)
* **tracker-lens:** rewrite bdd-scenarios' metric cards to match its own pie ([#147](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/147)) ([03a9c95](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/03a9c9506f0765e3a535991a61afa37a4c428d75)), closes [#147](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/147)
* **tracker-lens:** serialize release workflow runs to stop version races ([#156](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/156)) ([c4d9e79](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/c4d9e798b56ae06077dc6a2429f06eae9d08af44)), closes [#156](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/156)
* **tracker-lens:** use stageBreakdown for GHERKIN_SCENARIO metric cards ([#149](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/149)) ([6d7c99b](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/6d7c99b5ee21cf879f747184aed58c2572572221)), closes [#149](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/149)
* **ci:** Wait-For-Sibling-Releases passes --repo explicitly to gh run list ([#136](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/136)) ([b84e24e](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/b84e24e3728908e782ee02889ea2be70bcf0349f)), closes [#136](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/136) [#134](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/134)


### 📝 Documentation

* **tracker-lens:** add dashboard-extra-charts example (pie chart + nested Venn diagram) ([#143](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/143)) ([91423f3](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/91423f36fe57039b4c92081d3f955e6bfa87841e)), closes [#143](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/143) [#dashboard-data](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/dashboard-data)
* **tracker-lens:** add eight runnable examples and fix example TOC nesting ([#139](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/139)) ([92f6a2d](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/92f6a2d9fc85fa94bc0c5e9cab11a814c0db08d9)), closes [#139](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/139)
* **doppelganger-api-detector:** add verified-by-restdocs-restassured example ([#141](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/141)) ([0e88269](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/0e882691e014552337d163a0acfeed7c811b84bb)), closes [#141](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/141) [#140](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/140) [#140](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/140)
* **tracker-lens:** add Vulnerability Scan, Gradle Plugin Portal, and GitHub release badges ([#142](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/142)) ([8c0a602](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/8c0a602c2abe4eb4dbec0cfca98336563ca50f37)), closes [#142](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/142)


### 🔧 Misc

* **examples:** bump tracker-lens to 0.3.1 across examples ([#150](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/150)) ([cce801a](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/cce801a5bdecffae223e88450a34fa9cb26569e1)), closes [#150](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/150)


### BREAKING CHANGE

* **shadow-api-detector:** an existing contractHistoryFile (9 fields, no stubbedAt)
now fails detectShadowApis with a clear error instead of loading. Apply
mirage-api-detector and run its migrateContractHistory task once to
upgrade the file in place, or point contractHistoryFile at a new location
to start fresh.
* **mirage-api-detector:** an existing contractHistoryFile (9 fields, no stubbedAt)
now fails detectMirageApis with a clear error instead of loading. Run
migrateContractHistory once to upgrade it in place, or point
contractHistoryFile at a new location to start fresh.

## [1.4.2](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/shadow-api-detector-v1.4.1...shadow-api-detector-v1.4.2) (2026-08-13)


### 🐛 Bug Fixes

* **ci:** api-only-suite release workflow now fires on sibling-only releases ([#134](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/134)) ([8416a51](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/8416a51d1bce45f735f16e67024a7469f1b16aea)), closes [#134](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/134)


### 📝 Documentation

* add AI/tooling-oriented history file format references to all four plugins ([#135](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/135)) ([e4c88ba](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/e4c88ba1e26b24d3972be2492ce6ec0ce493aa65)), closes [#135](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/135)

## [1.4.1](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/shadow-api-detector-v1.4.0...shadow-api-detector-v1.4.1) (2026-08-13)


### 🐛 Bug Fixes

* **api-only-suite:** detectAllApiGaps no longer fails when an early detector finds a gap ([#132](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/132)) ([7fea3e7](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/7fea3e7a02508efb7d97bb7c899512b4636659eb)), closes [#132](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/132)
* renaming the history file path no longer leaves history-tracking tasks UP-TO-DATE ([#133](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/133)) ([9d7e7d6](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/9d7e7d653f207440acf61cd8fe6437707b48e7d2)), closes [#133](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/133)

# [1.4.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/shadow-api-detector-v1.3.0...shadow-api-detector-v1.4.0) (2026-08-13)


### ✨ New and updated features

* **gherkin-to-asciidoc:** log LIFECYCLE-level scan progress during feature docs generation ([#128](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/128)) ([1e269bd](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/1e269bdc496508e5083e69ee521fa1da1c26c7df)), closes [#128](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/128) [#56](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/56) [#56](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/56)
* track contract progress history and report it over time in all three API detector plugins ([#131](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/131)) ([e65f7bf](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/e65f7bf72557402f4078dcfa7abc0a10436103db)), closes [#131](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/131) [#59](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/59) [#59](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/59)


### 🐛 Bug Fixes

* **gherkin-to-asciidoc:** render Tracked since as a human-friendly timestamp ([#130](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/130)) ([96b50cd](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/96b50cd231a02e973be091e57d005373c52eb2be)), closes [#130](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/130)
* **api-only-suite:** wait for sibling detector releases before resolving their published version ([#129](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/129)) ([db3d8c9](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/db3d8c9b86e48f522ee1864c3220b746af5f39fa)), closes [#129](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/129)

# [1.3.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/shadow-api-detector-v1.2.0...shadow-api-detector-v1.3.0) (2026-08-12)


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

# [1.2.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/shadow-api-detector-v1.1.0...shadow-api-detector-v1.2.0) (2026-08-12)


### ✨ New and updated features

* add Doppelganger API Detector and Arc-E-Tect API-Only Suite plugins ([#111](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/111)) ([f59ee4d](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/f59ee4dba895f13ad8f49fb3de893c4304bd2645)), closes [#111](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/111)


### 🐛 Bug Fixes

* **doppelganger-api-detector:** allow manually dispatching the release workflow ([#113](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/113)) ([714b739](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/714b739a79875cf238a1833d79eee15b648563c9)), closes [#113](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/113)
* depend on the published api-detector-core artifact ([#114](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/114)) ([13313e6](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/13313e683f0519f446c82f0333b7030024a468b8)), closes [#114](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/114)

# [1.1.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/compare/shadow-api-detector-v1.0.0...shadow-api-detector-v1.1.0) (2026-08-11)


### ✨ New and updated features

* **mirage-api-detector:** add Mirage API Detector Gradle plugin ([#107](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/107)) ([4606b17](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/4606b17e1aa73c69013766557e765a793692a8b8)), closes [#107](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/107)


### 🐛 Bug Fixes

* **mirage-api-detector:** treat blank operationId/tags as missing in the report ([#108](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/108)) ([eb5f4fa](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/eb5f4fac324f32a04a17049f98a1cca8620b7c94)), closes [#108](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/108)


### 📝 Documentation

* **mirage-api-detector:** add example projects using the published plugin ([#109](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/109)) ([57cc669](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/57cc6691899d57a19780a5339f026d725ad9a70c)), closes [#109](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/109)
* **shadow-api-detector:** wire with-shadow-apis example into check explicitly ([#106](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/106)) ([9d75337](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/9d753370456988f7f6b268d3b8fea79e622bebfe)), closes [#106](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/106)


### 🔧 Misc

* dependency updates for Gradle plugin repository ([#110](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/110)) ([4408aa8](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/commit/4408aa829b242dba8e3807474dd1225e39493c32)), closes [#110](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/110) [#110](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/issues/110)

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
