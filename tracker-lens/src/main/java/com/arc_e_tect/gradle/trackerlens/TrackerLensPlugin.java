package com.arc_e_tect.gradle.trackerlens;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskProvider;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Gradle plugin that generates a themeable, standalone HTML dashboard from Gherkin scenario and API
 * contract lifecycle history.
 *
 * <p>Registers the {@code trackerLens} DSL extension, a {@code lensStyle} resolvable configuration
 * that external style packs are declared against, the {@code generateTrackerLensDashboard} task, and
 * the {@code listTrackerLensStyles} task. Neither task is wired into {@code check} or {@code build}
 * automatically - the dashboard is a report, not a verification gate.</p>
 *
 * <h2>Usage</h2>
 * <pre>
 * plugins {
 *     id 'com.arc-e-tect.tracker-lens'
 * }
 *
 * trackerLens {
 *     trackers {
 *         register("bdd-scenarios") { historyFiles.from(file("gherkin-progress-history.ndjson")); source = TrackerSourceKind.GHERKIN_SCENARIO }
 *     }
 * }
 * </pre>
 */
public class TrackerLensPlugin implements Plugin<Project> {

    /** Name of the dashboard-generating task registered by this plugin. */
    public static final String TASK_NAME = "generateTrackerLensDashboard";

    /** Name of the lens-listing task registered by this plugin. */
    public static final String LIST_STYLES_TASK_NAME = "listTrackerLensStyles";

    /** Creates a new plugin instance. Instantiated by Gradle infrastructure. */
    public TrackerLensPlugin() {}

    @Override
    public void apply(Project project) {
        TrackerLensExtension extension = project.getExtensions().create(TrackerLensExtension.NAME, TrackerLensExtension.class);
        extension.getOutputDir().convention(project.getLayout().getBuildDirectory().dir("reports/tracker-lens"));

        Configuration lensStyle = project.getConfigurations().create(TrackerLensExtension.LENS_STYLE_CONFIGURATION_NAME, c -> {
            c.setCanBeConsumed(false);
            c.setCanBeResolved(true);
            c.setDescription("External tracker-lens style packs contributing named lenses.");
        });

        TaskProvider<GenerateTrackerLensTask> generateTask = project.getTasks().register(
                TASK_NAME, GenerateTrackerLensTask.class, task -> {
                    task.getOutputDirectory().set(extension.getOutputDir());
                    task.getLensStylesheet().set(extension.getLensStylesheet());
                    task.getPreferredLensPack().set(extension.getPreferredLensPack());
                    task.getDefaultLens().set(extension.getDefaultLens());
                    task.getTemplate().set(extension.getTemplate());
                    task.getLensStyleClasspath().from(lensStyle);
                });

        // Needs no tracker to be registered at all, so it is wired outside afterEvaluate and can
        // never be affected by the at-least-one-tracker validation below.
        project.getTasks().register(LIST_STYLES_TASK_NAME, ListTrackerLensStylesTask.class, task -> {
            task.getLensStylesheet().set(extension.getLensStylesheet());
            task.getPreferredLensPack().set(extension.getPreferredLensPack());
            task.getLensStyleClasspath().from(lensStyle);
        });

        project.afterEvaluate(p -> {
            if (extension.getTrackers().isEmpty()) {
                throw new GradleException(
                        "trackerLens: at least one tracker must be registered under trackers { register(\"...\") { ... } }");
            }
            List<TrackerSpec> specs = extension.getTrackers().stream()
                    .map(this::toTrackerSpec)
                    .collect(Collectors.toList());
            generateTask.configure(task -> task.getTrackerSpecs().set(specs));
        });
    }

    private TrackerSpec toTrackerSpec(TrackerRegistration registration) {
        if (registration.getHistoryFiles().isEmpty()) {
            throw new GradleException(
                    "trackerLens: tracker \"" + registration.getName() + "\" has no historyFiles configured");
        }
        if (!registration.getSource().isPresent()) {
            throw new GradleException(
                    "trackerLens: tracker \"" + registration.getName() + "\" has no source configured");
        }
        return new TrackerSpec(
                registration.getName(), registration.getSource().get(),
                List.copyOf(registration.getHistoryFiles().getFiles()));
    }
}
