package com.arc_e_tect.gradle.trackerlens;

import com.arc_e_tect.gradle.dslupdater.DslExtensionSchema;
import com.arc_e_tect.gradle.dslupdater.DslPropertySpec;

import java.util.List;

/**
 * The {@code trackerLens {}} block's DSL property schema for {@code updateTrackerLensDSL} (see
 * {@code com.arc_e_tect.gradle.dslupdater.DslUpdater}).
 *
 * <p>Only lists properties that {@link TrackerLensPlugin#apply} actually gives a resolved
 * default via {@code convention(...)}: {@code outputDir}, {@code dashboardName}, and
 * {@code version}. {@code lensStylesheet}, {@code preferredLensPack}, {@code defaultLens},
 * {@code template}, and {@code templateId} are left out on purpose - each is genuinely optional
 * with no default value at all (unset just means "don't use this feature"), so there's nothing a
 * default literal could correctly represent for them. Each literal below is the same value that
 * property's own {@code convention(...)} call in {@link TrackerLensPlugin#apply} resolves to, so
 * that writing it out explicitly is always a no-op.</p>
 */
final class TrackerLensDslSchema {

    static final DslExtensionSchema SCHEMA = new DslExtensionSchema(TrackerLensExtension.NAME, List.of(
            DslPropertySpec.scalar("outputDir", "layout.buildDirectory.dir('reports/tracker-lens')",
                    "Where dashboard.html and its lens CSS files are written."),
            DslPropertySpec.scalar("dashboardName", "\"${project.name} Lens\"",
                    "The dashboard's displayed name, shown in the browser tab title and the page heading."),
            DslPropertySpec.scalar("version", "project.version.toString()",
                    "Version shown alongside the dashboard's name."),
            DslPropertySpec.container("trackers", "At least one tracker must be registered.",
                    "// register('bdd-scenarios') {\n"
                            + "//     historyFiles.from(file('gherkin-progress-history.ndjson'))\n"
                            + "//     source = com.arc_e_tect.gradle.trackerlens.TrackerSourceKind.GHERKIN_SCENARIO\n"
                            + "// }")
    ));

    private TrackerLensDslSchema() {
    }
}
