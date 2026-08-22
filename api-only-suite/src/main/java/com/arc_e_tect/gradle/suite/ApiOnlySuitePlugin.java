package com.arc_e_tect.gradle.suite;

import com.arc_e_tect.gradle.doppelganger.DetectDoppelgangerApisTask;
import com.arc_e_tect.gradle.doppelganger.DoppelgangerApiDetectorExtension;
import com.arc_e_tect.gradle.doppelganger.DoppelgangerApiDetectorPlugin;
import com.arc_e_tect.gradle.mirage.DetectMirageApisTask;
import com.arc_e_tect.gradle.mirage.MirageApiDetectorExtension;
import com.arc_e_tect.gradle.mirage.MirageApiDetectorPlugin;
import com.arc_e_tect.gradle.shadow.DetectShadowApisTask;
import com.arc_e_tect.gradle.shadow.ShadowApiDetectorExtension;
import com.arc_e_tect.gradle.shadow.ShadowApiDetectorPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

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
 * still in flight to finish first, so this suite is never published pinned to a sibling version
 * older than what that same change actually released. This suite's own release workflow fires both
 * when this plugin's own files change and whenever any sibling's release workflow completes, so a
 * sibling-only change (no file under this module touched at all) still reaches a new release of
 * this suite - not just changes to this module's own source.</p>
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
 *
 * <h2>{@code detectAllApiGaps} never fails the build on a detected gap</h2>
 * <p>{@code detectAllApiGaps} always runs all three detectors to completion and never fails the
 * build, regardless of {@code failOnShadow}/{@code failOnMirage}/{@code failOnDoppelganger} -
 * including when any of the three is {@code true} because of
 * {@link ApiOnlySuiteExtension#getFailOnDetection()}. It depends on three dedicated task instances
 * - not {@code detectShadowApis}/{@code detectMirageApis}/{@code detectDoppelgangerApis} themselves
 * - configured identically except with their own fail-on-gap property forced to {@code false}, so a
 * shadow API found first can never prevent Mirage or Doppelganger from running. Run the individual
 * {@code detectShadowApis}/{@code detectMirageApis}/{@code detectDoppelgangerApis} tasks directly
 * (or wire them into {@code check} individually) when you want the build to actually fail on a
 * detected gap - {@code failOnDetection = true} is exactly how to make all three do so at once.</p>
 */
public class ApiOnlySuitePlugin implements Plugin<Project> {

    /** Name of the aggregate Gradle task registered by this plugin. */
    public static final String TASK_NAME = "detectAllApiGaps";

    /** Creates a new plugin instance. Instantiated by Gradle infrastructure. */
    public ApiOnlySuitePlugin() {}

    @Override
    public void apply(Project project) {
        ApiOnlySuiteExtension ext = project.getExtensions().create(ApiOnlySuiteExtension.NAME, ApiOnlySuiteExtension.class);
        ext.getFailOnDetection().convention(false);

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

        TaskProvider<DetectShadowApisTask> shadowForSuite = registerNonFailingShadowTask(project);
        TaskProvider<DetectMirageApisTask> mirageForSuite = registerNonFailingMirageTask(project);
        TaskProvider<DetectDoppelgangerApisTask> doppelgangerForSuite = registerNonFailingDoppelgangerTask(project);

        project.getTasks().register(TASK_NAME, task -> {
            task.setGroup("verification");
            task.setDescription("Runs all three Arc-E-Tect API-Only detectors: Shadow, Mirage, and Doppelganger. "
                    + "Never fails the build on a detected gap, regardless of failOnShadow/failOnMirage/"
                    + "failOnDoppelganger - run the individual tasks directly to enforce failure.");
            task.dependsOn(shadowForSuite, mirageForSuite, doppelgangerForSuite);
        });
    }

    /**
     * Registers a {@code shadowApiGapsForSuite} task instance, wired identically to
     * {@code detectShadowApis} except with {@code failOnShadow} forced to {@code false}, so that
     * {@link #TASK_NAME} can depend on it without ever risking a build failure that would prevent
     * Mirage and Doppelganger's own tasks from running.
     *
     * @param project the project {@link #apply(Project)} was called on
     * @return the registered non-failing task
     */
    private TaskProvider<DetectShadowApisTask> registerNonFailingShadowTask(Project project) {
        TaskProvider<DetectShadowApisTask> primary =
                project.getTasks().named(ShadowApiDetectorPlugin.TASK_NAME, DetectShadowApisTask.class);
        return project.getTasks().register("shadowApiGapsForSuite", DetectShadowApisTask.class, task -> {
            DetectShadowApisTask source = primary.get();
            task.getControllerDirs().from(source.getControllerDirs());
            task.getRootDocument().set(source.getRootDocument());
            task.getOpenApiDir().set(source.getOpenApiDir());
            task.getFailOnShadow().set(false);
            task.getReportDir().set(source.getReportDir());
            task.getReportFileName().set(source.getReportFileName());
            task.getSystemUnderTestVersion().set(source.getSystemUnderTestVersion());
            task.getTrackContractHistory().set(source.getTrackContractHistory());
            task.getContractHistoryFile().set(source.getContractHistoryFile());
            task.getUpdateContractHistory().set(source.getUpdateContractHistory());
        });
    }

    /**
     * Registers a {@code mirageApiGapsForSuite} task instance, wired identically to
     * {@code detectMirageApis} except with {@code failOnMirage} forced to {@code false}, so that
     * {@link #TASK_NAME} can depend on it without ever risking a build failure that would prevent
     * Shadow and Doppelganger's own tasks from running.
     *
     * @param project the project {@link #apply(Project)} was called on
     * @return the registered non-failing task
     */
    private TaskProvider<DetectMirageApisTask> registerNonFailingMirageTask(Project project) {
        TaskProvider<DetectMirageApisTask> primary =
                project.getTasks().named(MirageApiDetectorPlugin.TASK_NAME, DetectMirageApisTask.class);
        return project.getTasks().register("mirageApiGapsForSuite", DetectMirageApisTask.class, task -> {
            DetectMirageApisTask source = primary.get();
            task.getControllerDirs().from(source.getControllerDirs());
            task.getScanMocks().set(source.getScanMocks());
            task.getStubDirs().from(source.getStubDirs());
            task.getBasePath().set(source.getBasePath());
            task.getRootDocument().set(source.getRootDocument());
            task.getOpenApiDir().set(source.getOpenApiDir());
            task.getFailOnMirage().set(false);
            task.getReportDir().set(source.getReportDir());
            task.getReportFileName().set(source.getReportFileName());
            task.getSystemUnderTestVersion().set(source.getSystemUnderTestVersion());
            task.getTrackContractHistory().set(source.getTrackContractHistory());
            task.getContractHistoryFile().set(source.getContractHistoryFile());
            task.getUpdateContractHistory().set(source.getUpdateContractHistory());
        });
    }

    /**
     * Registers a {@code doppelgangerApiGapsForSuite} task instance, wired identically to
     * {@code detectDoppelgangerApis} except with {@code failOnDoppelganger} forced to {@code false},
     * so that {@link #TASK_NAME} can depend on it without ever risking a build failure that would
     * prevent Shadow and Mirage's own tasks from running.
     *
     * @param project the project {@link #apply(Project)} was called on
     * @return the registered non-failing task
     */
    private TaskProvider<DetectDoppelgangerApisTask> registerNonFailingDoppelgangerTask(Project project) {
        TaskProvider<DetectDoppelgangerApisTask> primary = project.getTasks()
                .named(DoppelgangerApiDetectorPlugin.TASK_NAME, DetectDoppelgangerApisTask.class);
        return project.getTasks().register("doppelgangerApiGapsForSuite", DetectDoppelgangerApisTask.class, task -> {
            DetectDoppelgangerApisTask source = primary.get();
            task.getControllerDirs().from(source.getControllerDirs());
            task.getTestDirs().from(source.getTestDirs());
            task.getRootDocument().set(source.getRootDocument());
            task.getOpenApiDir().set(source.getOpenApiDir());
            task.getContractsDir().set(source.getContractsDir());
            task.getUseRestDocs().set(source.getUseRestDocs());
            task.getUseOpenApiRequestValidator().set(source.getUseOpenApiRequestValidator());
            task.getUseSpringCloudContract().set(source.getUseSpringCloudContract());
            task.getFailOnDoppelganger().set(false);
            task.getReportDir().set(source.getReportDir());
            task.getReportFileName().set(source.getReportFileName());
            task.getSystemUnderTestVersion().set(source.getSystemUnderTestVersion());
            task.getTrackContractHistory().set(source.getTrackContractHistory());
            task.getContractHistoryFile().set(source.getContractHistoryFile());
            task.getUpdateContractHistory().set(source.getUpdateContractHistory());
        });
    }

    /**
     * Forwards {@link ApiOnlySuiteExtension#getRootDocument()},
     * {@link ApiOnlySuiteExtension#getControllerDirs()}, and
     * {@link ApiOnlySuiteExtension#getFailOnDetection()} into each of the three underlying
     * extensions, as a fallback that only applies where the consumer has not already configured
     * that property directly on the individual extension.
     *
     * <p>{@code rootDocument} and {@code failOnDetection} are forwarded via
     * {@link org.gradle.api.provider.Property#convention}, which - unlike
     * {@link org.gradle.api.provider.Property#set} - always yields to an explicit value regardless
     * of the order the two are configured in, exactly the way {@code ShadowApiDetectorPlugin}
     * already uses {@code convention(...)} for its own defaulting. This replaces each plugin's own
     * {@code false} convention for its fail-on-gap property with one that tracks
     * {@code failOnDetection} instead - not a behavior change for the common case, since
     * {@code failOnDetection} itself defaults to {@code false} too, but it means an individual
     * plugin's own explicit fail-on-gap value (set before or after this callback runs) still always
     * wins, per {@code Property#convention}'s own guarantee. {@code ConfigurableFileCollection} has
     * no equivalent lazy-fallback mechanism, so {@code controllerDirs} is forwarded with the same
     * eager empty-check idiom the three detector plugins already use for their own
     * {@code src/main/java} default - which is exactly why this method must run before those
     * plugins' own defaulting does.</p>
     */
    private void forwardSharedSettings(Project project, ApiOnlySuiteExtension ext) {
        ShadowApiDetectorExtension shadowExt = project.getExtensions().getByType(ShadowApiDetectorExtension.class);
        MirageApiDetectorExtension mirageExt = project.getExtensions().getByType(MirageApiDetectorExtension.class);
        DoppelgangerApiDetectorExtension doppelgangerExt =
                project.getExtensions().getByType(DoppelgangerApiDetectorExtension.class);

        shadowExt.getRootDocument().convention(ext.getRootDocument());
        mirageExt.getRootDocument().convention(ext.getRootDocument());
        doppelgangerExt.getRootDocument().convention(ext.getRootDocument());

        shadowExt.getFailOnShadow().convention(ext.getFailOnDetection());
        mirageExt.getFailOnMirage().convention(ext.getFailOnDetection());
        doppelgangerExt.getFailOnDoppelganger().convention(ext.getFailOnDetection());

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
