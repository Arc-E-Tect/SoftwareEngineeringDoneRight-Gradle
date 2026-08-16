package com.arc_e_tect.gradle.trackerlens;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates a complete, runnable pair of projects for developing and testing a new lens end to
 * end - the same boilerplate {@code LENS_TUTORIAL.adoc} walks through by hand: a minimal lens-pack
 * project (containing the same boilerplate CSS {@code initTrackerLens} generates) and a minimal
 * sample project that applies {@code tracker-lens} itself, already wired to consume that pack and,
 * on its first (and every) dashboard build, generate its own calibrated sample fixture via
 * {@code generateTrackerLensFixture} rather than reading a static file this task writes once.
 *
 * <p>The sample project pins the {@code tracker-lens} version that generated it (via
 * {@link PluginMetadata}), so regenerating after upgrading the plugin picks up the new version
 * automatically instead of a hand-maintained, driftable placeholder.</p>
 */
@DisableCachingByDefault(because = "Scaffolds a multi-file project tree as a one-time developer convenience, not a build output")
public abstract class BootstrapTrackerLensProjectTask extends DefaultTask {

    /** Creates a new task instance. Instantiated by Gradle infrastructure. */
    @Inject
    public BootstrapTrackerLensProjectTask() {
        setGroup("tracker lens");
        setDescription("Generates a boilerplate lens-pack project and a sample consumer project for developing "
                + "and testing a new lens end-to-end.");
    }

    /**
     * Where the {@code lens-pack} and {@code sample-project} directories are written.
     *
     * @return mutable directory property for the output directory
     */
    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    /** Generates both projects, refusing to write into a non-empty output directory. */
    @TaskAction
    public void bootstrap() {
        File root = getOutputDir().get().getAsFile();
        String[] existingEntries = root.list();
        if (root.exists() && existingEntries != null && existingEntries.length > 0) {
            throw new GradleException("trackerLens: " + root + " already exists and is not empty - remove it or "
                    + "configure a different outputDir before running bootstrapTrackerLensProject again, so "
                    + "existing work is never silently overwritten.");
        }

        File lensPackDir = new File(root, "lens-pack");
        File sampleProjectDir = new File(root, "sample-project");

        writeFile(new File(root, "README.adoc"), BootstrapProjectFiles.topLevelReadme());

        writeFile(new File(lensPackDir, "settings.gradle"), BootstrapProjectFiles.lensPackSettingsGradle());
        writeFile(new File(lensPackDir, "build.gradle"), BootstrapProjectFiles.lensPackBuildGradle());
        writeFile(new File(lensPackDir, "README.adoc"), BootstrapProjectFiles.lensPackReadme());
        writeLensBoilerplate(new File(lensPackDir,
                "src/main/resources/META-INF/arc-e-tect/tracker-lens/lenses/my-lens.css"));

        writeFile(new File(sampleProjectDir, "settings.gradle"), BootstrapProjectFiles.sampleProjectSettingsGradle());
        writeFile(new File(sampleProjectDir, "build.gradle"),
                BootstrapProjectFiles.sampleProjectBuildGradle(PluginMetadata.pluginVersion()));
        writeFile(new File(sampleProjectDir, "README.adoc"), BootstrapProjectFiles.sampleProjectReadme());

        getLogger().lifecycle("trackerLens: wrote a lens-pack project and a sample consumer project to {}", root);
        getLogger().lifecycle("trackerLens: see {} for how to run them.", new File(root, "README.adoc"));
    }

    private void writeLensBoilerplate(File target) {
        try {
            Files.createDirectories(target.toPath().getParent());
            try (InputStream stream = getClass().getClassLoader()
                    .getResourceAsStream(InitTrackerLensTask.BOILERPLATE_RESOURCE)) {
                if (stream == null) {
                    throw new GradleException("trackerLens: missing bundled lens boilerplate resource: "
                            + InitTrackerLensTask.BOILERPLATE_RESOURCE);
                }
                Files.copy(stream, target.toPath());
            }
        } catch (IOException e) {
            throw new GradleException("trackerLens: failed to write " + target, e);
        }
    }

    private void writeFile(File target, String content) {
        try {
            Path targetPath = target.toPath();
            Files.createDirectories(targetPath.getParent());
            Files.writeString(targetPath, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GradleException("trackerLens: failed to write " + target, e);
        }
    }
}
