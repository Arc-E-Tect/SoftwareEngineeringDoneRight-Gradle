package com.arc_e_tect.gradle.trackerlens;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gradle plugin that generates a themeable, standalone HTML dashboard from Gherkin scenario and API
 * contract lifecycle history.
 *
 * <p>Registers the {@code trackerLens} DSL extension, a {@code lensStyle} resolvable configuration
 * that external style packs are declared against, the {@code generateTrackerLensDashboard} task, the
 * {@code listTrackerLensStyles} task, and the {@code initTrackerLens} / {@code bootstrapTrackerLensProject}
 * scaffolding tasks. None of these tasks is wired into {@code check} or {@code build} automatically -
 * the dashboard is a report, not a verification gate, and the scaffolding tasks are one-time developer
 * conveniences.</p>
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

    /** Name of the lens-boilerplate-generating task registered by this plugin. */
    public static final String INIT_LENS_TASK_NAME = "initTrackerLens";

    /** Name of the project-scaffolding task registered by this plugin. */
    public static final String BOOTSTRAP_PROJECT_TASK_NAME = "bootstrapTrackerLensProject";

    /** Creates a new plugin instance. Instantiated by Gradle infrastructure. */
    public TrackerLensPlugin() {}

    @Override
    public void apply(Project project) {
        TrackerLensExtension extension = project.getExtensions().create(TrackerLensExtension.NAME, TrackerLensExtension.class);
        extension.getOutputDir().convention(project.getLayout().getBuildDirectory().dir("reports/tracker-lens"));
        extension.getDashboardName().convention(project.provider(() -> project.getName() + " Lens"));
        extension.getVersion().convention(project.provider(() -> String.valueOf(project.getVersion())));

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
                    task.getDashboardName().set(extension.getDashboardName());
                    task.getVersion().set(extension.getVersion());
                    task.getLensStyleClasspath().from(lensStyle);
                });

        // None of the three tasks below needs a tracker to be registered at all, so all three are
        // wired outside afterEvaluate and can never be affected by the at-least-one-tracker
        // validation below.
        project.getTasks().register(LIST_STYLES_TASK_NAME, ListTrackerLensStylesTask.class, task -> {
            task.getLensStylesheet().set(extension.getLensStylesheet());
            task.getPreferredLensPack().set(extension.getPreferredLensPack());
            task.getLensStyleClasspath().from(lensStyle);
        });

        project.getTasks().register(INIT_LENS_TASK_NAME, InitTrackerLensTask.class, task ->
                task.getLensFile().convention(project.getLayout().getProjectDirectory()
                        .file("src/main/resources/META-INF/arc-e-tect/tracker-lens/lenses/my-lens.css")));

        project.getTasks().register(BOOTSTRAP_PROJECT_TASK_NAME, BootstrapTrackerLensProjectTask.class, task ->
                task.getOutputDir().convention(project.getLayout().getProjectDirectory().dir("tracker-lens-bootstrap")));

        // Collected eagerly as each tracker is registered (all() realizes and invokes its action
        // immediately, unlike register()'s own deferred realization) rather than read back from
        // the container inside afterEvaluate below: Gradle's mutation guard forbids realizing a
        // container's still-pending register()-deferred elements from within an afterEvaluate
        // listener callback, so iterating extension.getTrackers() itself at that point fails with
        // "NamedDomainObjectContainer#create(String) ... cannot be executed in the current
        // context" the moment a build actually applies the plugin (a ProjectBuilder-based unit
        // test does not enforce this guard, so this class of bug is invisible to one - only a
        // real Gradle invocation, e.g. via TestKit, catches it).
        List<TrackerRegistration> registrations = new ArrayList<>();
        extension.getTrackers().all(registrations::add);

        // The "at least one tracker" requirement is deliberately enforced inside
        // GenerateTrackerLensTask's own task action, not here in afterEvaluate: throwing here
        // would fail the whole project's configuration - and therefore every task, including the
        // three registered above that need no tracker at all - the moment zero trackers are
        // registered, rather than failing only when generateTrackerLensDashboard itself runs.
        project.afterEvaluate(p -> {
            List<TrackerSpec> specs = registrations.stream()
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
