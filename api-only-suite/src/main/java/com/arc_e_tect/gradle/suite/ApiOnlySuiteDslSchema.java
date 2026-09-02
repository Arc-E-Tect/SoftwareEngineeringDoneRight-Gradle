package com.arc_e_tect.gradle.suite;

import com.arc_e_tect.gradle.dslupdater.DslExtensionSchema;
import com.arc_e_tect.gradle.dslupdater.DslPropertySpec;

import java.util.List;

/**
 * The {@code apiOnlySuite {}} block's DSL property schema for {@code updateApiOnlySuiteDSL} (see
 * {@code com.arc_e_tect.gradle.dslupdater.DslUpdater}).
 *
 * <p>Only lists properties that {@link ApiOnlySuitePlugin#apply} actually gives a resolved default
 * via {@code convention(...)} <em>on this extension's own properties</em> - {@code failOnDetection},
 * {@code excludePaths}, and {@code excludeWellKnown}. {@code rootDocument}, {@code controllerDirs},
 * and {@code excludeFiles} are left out on purpose: each is a fallback forwarded into the three
 * sibling detector plugins' own extensions only when a consumer hasn't configured that plugin's own
 * property directly - {@code apiOnlySuite}'s own copy has no default of its own to add, only "unset,
 * so nothing is forwarded". Each literal below is the same value that property's own
 * {@code convention(...)} call in {@link ApiOnlySuitePlugin#apply} resolves to, so that writing it
 * out explicitly is always a no-op.</p>
 *
 * <p>This schema covers {@code apiOnlySuite}'s own DSL block only - it does not touch the three
 * sibling plugins' own {@code shadowApiDetector {}} / {@code mirageApiDetector {}} /
 * {@code doppelgangerApiDetector {}} blocks, each of which has its own {@code updateDSL} task.</p>
 */
final class ApiOnlySuiteDslSchema {

    static final DslExtensionSchema SCHEMA = new DslExtensionSchema(ApiOnlySuiteExtension.NAME, List.of(
            DslPropertySpec.scalar("failOnDetection", "false",
                    "Fallback fail-on-gap value forwarded to all three underlying plugins, when not configured "
                            + "directly on the individual plugin's own extension."),
            DslPropertySpec.scalar("excludePaths", "[]",
                    "Exclusion rule strings, shared by all three underlying detector plugins as a fallback."),
            DslPropertySpec.scalar("excludeWellKnown", "[]",
                    "Names of bundled well-known exclusion sets, shared by all three underlying detector plugins "
                            + "as a fallback, e.g. 'spring-boot-actuator'.")
    ));

    private ApiOnlySuiteDslSchema() {
    }
}
