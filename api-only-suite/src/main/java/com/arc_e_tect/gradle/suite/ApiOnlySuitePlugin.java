package com.arc_e_tect.gradle.suite;

import com.arc_e_tect.gradle.doppelganger.DoppelgangerApiDetectorExtension;
import com.arc_e_tect.gradle.doppelganger.DoppelgangerApiDetectorPlugin;
import com.arc_e_tect.gradle.mirage.MirageApiDetectorExtension;
import com.arc_e_tect.gradle.mirage.MirageApiDetectorPlugin;
import com.arc_e_tect.gradle.shadow.ShadowApiDetectorExtension;
import com.arc_e_tect.gradle.shadow.ShadowApiDetectorPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Gradle plugin that applies the Shadow, Mirage, and Doppelganger API Detector plugins together,
 * and registers the {@code detectAllApiGaps} aggregate task and the {@code apiOnlySuite} DSL
 * extension.
 *
 * <p>This is a pure composition module: it contains no detection logic of its own, only wiring.
 * Each of the three underlying plugins registers its own task and extension exactly as it does
 * when applied on its own - applying via this suite is indistinguishable, from each individual
 * plugin's point of view, from a consumer applying it directly. None of the three plugins was
 * modified to support being applied this way.</p>
 *
 * <p>Applying this plugin pulls in whatever version of each of the three detector plugins was the
 * latest published one at the time this suite's own version was released - not necessarily each
 * plugin's current latest, since a consumer only re-resolves it by upgrading the suite itself. The
 * release pipeline resolves each sibling's real published version from its own release tags
 * immediately before publishing this plugin, and - since Aug 2026 - waits for any sibling release
 * this same push also triggers to finish first, so this suite is never published pinned to a
 * sibling version older than what that same change actually released.</p>
 *
 * <h2>Usage</h2>
 * <pre>
 * plugins {
 *     id 'com.arc-e-tect.api-only-suite'
 * }
 *
 * apiOnlySuite {
 *     rootDocument = file('src/main/resources/openapi/openapi.yaml')
 * }
 * </pre>
 *
 * <p>The aggregate task is <strong>not</strong> wired into {@code check} or {@code build}
 * automatically, for the same reason none of the three individual plugins are. Opt in explicitly
 * once the task is safe to run as part of your build:</p>
 * <pre>
 * tasks.named('check') {
 *     dependsOn 'detectAllApiGaps'
 * }
 * </pre>
 */
public class ApiOnlySuitePlugin implements Plugin<Project> {

    /** Name of the aggregate Gradle task registered by this plugin. */
    public static final String TASK_NAME = "detectAllApiGaps";

    /** Creates a new plugin instance. Instantiated by Gradle infrastructure. */
    public ApiOnlySuitePlugin() {}

    @Override
    public void apply(Project project) {
        ApiOnlySuiteExtension ext = project.getExtensions().create(ApiOnlySuiteExtension.NAME, ApiOnlySuiteExtension.class);

        // Registered before the three detector plugins are applied below, so this callback runs
        // before each detector's own afterEvaluate-based defaulting of controllerDirs (afterEvaluate
        // callbacks fire in registration order). Forwarding must land as each extension's value - or
        // be skipped because the consumer already set one directly - before a detector plugin gets a
        // chance to decide its own controllerDirs is still unset and fill in its own src/main/java
        // default; otherwise a suite-configured directory would silently never reach the task.
        project.afterEvaluate(p -> forwardSharedSettings(p, ext));

        project.getPluginManager().apply(ShadowApiDetectorPlugin.class);
        project.getPluginManager().apply(MirageApiDetectorPlugin.class);
        project.getPluginManager().apply(DoppelgangerApiDetectorPlugin.class);

        project.getTasks().register(TASK_NAME, task -> {
            task.setGroup("verification");
            task.setDescription("Runs all three Arc-E-Tect API-Only detectors: Shadow, Mirage, and Doppelganger.");
            task.dependsOn(
                    ShadowApiDetectorPlugin.TASK_NAME,
                    MirageApiDetectorPlugin.TASK_NAME,
                    DoppelgangerApiDetectorPlugin.TASK_NAME);
        });
    }

    /**
     * Forwards {@link ApiOnlySuiteExtension#getRootDocument()} and
     * {@link ApiOnlySuiteExtension#getControllerDirs()} into each of the three underlying
     * extensions, as a fallback that only applies where the consumer has not already configured
     * that property directly on the individual extension.
     *
     * <p>{@code rootDocument} is forwarded via {@link org.gradle.api.provider.Property#convention},
     * which - unlike {@link org.gradle.api.provider.Property#set} - always yields to an explicit
     * value regardless of the order the two are configured in, exactly the way
     * {@code ShadowApiDetectorPlugin} already uses {@code convention(...)} for its own defaulting.
     * {@code ConfigurableFileCollection} has no equivalent lazy-fallback mechanism, so
     * {@code controllerDirs} is forwarded with the same eager empty-check idiom the three detector
     * plugins already use for their own {@code src/main/java} default - which is exactly why this
     * method must run before those plugins' own defaulting does.</p>
     */
    private void forwardSharedSettings(Project project, ApiOnlySuiteExtension ext) {
        ShadowApiDetectorExtension shadowExt = project.getExtensions().getByType(ShadowApiDetectorExtension.class);
        MirageApiDetectorExtension mirageExt = project.getExtensions().getByType(MirageApiDetectorExtension.class);
        DoppelgangerApiDetectorExtension doppelgangerExt =
                project.getExtensions().getByType(DoppelgangerApiDetectorExtension.class);

        shadowExt.getRootDocument().convention(ext.getRootDocument());
        mirageExt.getRootDocument().convention(ext.getRootDocument());
        doppelgangerExt.getRootDocument().convention(ext.getRootDocument());

        if (shadowExt.getControllerDirs().isEmpty()) {
            shadowExt.getControllerDirs().from(ext.getControllerDirs());
        }
        if (mirageExt.getControllerDirs().isEmpty()) {
            mirageExt.getControllerDirs().from(ext.getControllerDirs());
        }
        if (doppelgangerExt.getControllerDirs().isEmpty()) {
            doppelgangerExt.getControllerDirs().from(ext.getControllerDirs());
        }
    }
}
