package com.arc_e_tect.gradle.trackerlens;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates a complete, heavily-commented boilerplate lens CSS file to start a new lens from,
 * covering every selector {@code ContractRule} governs (see {@code DASHBOARD-THEMING.adoc}) with
 * inline comments explaining what each one is for and clearly-fake placeholder colors to replace.
 *
 * <p>Writes to the fixed resource path a lens must live under to be discovered
 * ({@code src/main/resources/META-INF/arc-e-tect/tracker-lens/lenses/my-lens.css} by default), so
 * the file this task generates is immediately a working (if unstyled) lens - see
 * {@code LENS_TUTORIAL.adoc} for the full walkthrough from here to a published style pack, or
 * {@code bootstrapTrackerLensProject} to generate the surrounding project as well.</p>
 */
@DisableCachingByDefault(because = "Refuses to overwrite an existing lens file; a cache restore would bypass that safety check")
public abstract class InitTrackerLensTask extends DefaultTask {

    static final String BOILERPLATE_RESOURCE = "templates/lens-boilerplate.css";

    /** Creates a new task instance. Instantiated by Gradle infrastructure. */
    @Inject
    public InitTrackerLensTask() {
        setGroup("tracker lens");
        setDescription("Generates a complete, heavily-commented boilerplate lens CSS file to start a new lens from.");
    }

    /**
     * Where the boilerplate lens CSS file is written.
     *
     * @return mutable file property for the generated lens file
     */
    @OutputFile
    public abstract RegularFileProperty getLensFile();

    /** Generates the boilerplate lens file, refusing to overwrite one that already exists. */
    @TaskAction
    public void init() {
        Path target = getLensFile().get().getAsFile().toPath();
        if (Files.exists(target)) {
            throw new GradleException("trackerLens: " + target + " already exists - remove it, rename it, or "
                    + "configure a different lensFile before running initTrackerLens again, so an existing lens "
                    + "is never silently overwritten.");
        }

        try {
            Files.createDirectories(target.getParent());
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(BOILERPLATE_RESOURCE)) {
                if (stream == null) {
                    throw new GradleException(
                            "trackerLens: missing bundled lens boilerplate resource: " + BOILERPLATE_RESOURCE);
                }
                Files.copy(stream, target);
            }
        } catch (IOException e) {
            throw new GradleException("trackerLens: failed to write " + target, e);
        }

        getLogger().lifecycle("trackerLens: wrote boilerplate lens to {}", target);
        getLogger().lifecycle(
                "trackerLens: see LENS_TUTORIAL.adoc and DASHBOARD-THEMING.adoc for how to customize and publish it.");
    }
}
