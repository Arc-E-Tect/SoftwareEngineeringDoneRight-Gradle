package com.arc_e_tect.gradle.trackerlens;

import com.arc_e_tect.gradle.trackerlens.lens.LensSetResolver;
import com.arc_e_tect.gradle.trackerlens.lens.ResolvedLens;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lists every Tracker Lens style that {@code generateTrackerLensDashboard} would offer in the
 * dashboard's lens switcher - this plugin's own built-in lenses, every resolved external
 * {@code lensStyle} pack, and the {@code lensStylesheet} lens when configured - without generating
 * a dashboard.
 *
 * <p>Resolves the exact same lens set as {@code generateTrackerLensDashboard}, via the shared
 * {@link LensSetResolver}, so this listing can never drift from what actually ends up in the
 * switcher. Unlike {@code generateTrackerLensDashboard}, this task needs no tracker to be
 * registered at all - it only touches lens resolution.</p>
 */
@DisableCachingByDefault(because = "Prints the available lens styles to the console; produces no outputs")
public abstract class ListTrackerLensStylesTask extends DefaultTask {

    private final LensSetResolver lensSetResolver = new LensSetResolver();

    /** Creates a new task instance. Instantiated by Gradle infrastructure. */
    @Inject
    public ListTrackerLensStylesTask() {
        setGroup("reporting");
        setDescription("Lists every Tracker Lens style (built-in, external lensStyle packs, and the custom "
                + "lensStylesheet when configured) that generateTrackerLensDashboard would offer.");
    }

    /**
     * Classpath entries (directories or jars) resolved from the {@code lensStyle} configuration.
     *
     * @return mutable file collection of external style-pack classpath roots
     */
    @Classpath
    public abstract ConfigurableFileCollection getLensStyleClasspath();

    /**
     * Optional single CSS file contributing the {@code custom-lens} lens.
     *
     * @return mutable file property for the custom lens stylesheet
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    @Optional
    public abstract RegularFileProperty getLensStylesheet();

    /**
     * Optional coordinate restricting which single external style pack's lenses are offered.
     *
     * @return mutable property for the preferred lens pack coordinate
     */
    @Input
    @Optional
    public abstract Property<String> getPreferredLensPack();

    /** Resolves the lens set and prints it to the console, grouped by source. */
    @TaskAction
    public void list() {
        File lensStylesheet = getLensStylesheet().isPresent() ? getLensStylesheet().get().getAsFile() : null;
        List<ResolvedLens> lenses =
                lensSetResolver.resolve(lensStylesheet, getLensStyleClasspath(), getPreferredLensPack().getOrElse(""));

        Map<String, List<String>> idsBySource = new LinkedHashMap<>();
        for (ResolvedLens lens : lenses) {
            idsBySource.computeIfAbsent(lens.sourceLabel(), key -> new ArrayList<>()).add(lens.id());
        }

        System.out.println("Tracker Lens styles available (" + lenses.size() + "):");
        for (Map.Entry<String, List<String>> entry : idsBySource.entrySet()) {
            System.out.println();
            System.out.println(displayName(entry.getKey()) + ":");
            for (String id : entry.getValue()) {
                System.out.println("  " + id);
            }
        }
    }

    private String displayName(String sourceLabel) {
        return switch (sourceLabel) {
            case "built-in" -> "Built-in";
            case "custom" -> "Custom (lensStylesheet)";
            default -> sourceLabel;
        };
    }
}
