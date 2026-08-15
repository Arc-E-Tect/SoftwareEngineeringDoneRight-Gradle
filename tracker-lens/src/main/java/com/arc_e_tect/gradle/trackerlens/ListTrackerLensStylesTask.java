package com.arc_e_tect.gradle.trackerlens;

import com.arc_e_tect.gradle.trackerlens.lens.LensSetResolver;
import com.arc_e_tect.gradle.trackerlens.lens.ResolvedLens;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lists every Tracker Lens style available through a lens pack - this plugin's own built-in lenses
 * and every resolved external {@code lensStyle} pack's lenses - without generating a dashboard.
 *
 * <p>Scoped to lens packs only: {@code trackerLens.lensStylesheet}'s single {@code custom-lens}
 * lens - a bare project-supplied CSS file, not a discoverable, id-namespaced pack - is a different
 * mechanism entirely and never appears here, even though {@code generateTrackerLensDashboard}'s own
 * lens switcher does still offer it. Resolves the lens-pack subset of the same lens set
 * {@code generateTrackerLensDashboard} resolves, via the shared {@link LensSetResolver} (called with
 * no {@code lensStylesheet}), so this listing can never drift from what the packs themselves
 * contribute. Needs no tracker to be registered at all - it only touches lens resolution.</p>
 */
@DisableCachingByDefault(because = "Prints the available lens styles to the console; produces no outputs")
public abstract class ListTrackerLensStylesTask extends DefaultTask {

    private final LensSetResolver lensSetResolver = new LensSetResolver();

    /** Creates a new task instance. Instantiated by Gradle infrastructure. */
    @Inject
    public ListTrackerLensStylesTask() {
        setGroup("reporting");
        setDescription("Lists every Tracker Lens style available through a lens pack (built-in and external "
                + "lensStyle packs).");
    }

    /**
     * Classpath entries (directories or jars) resolved from the {@code lensStyle} configuration.
     *
     * @return mutable file collection of external style-pack classpath roots
     */
    @Classpath
    public abstract ConfigurableFileCollection getLensStyleClasspath();

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
        List<ResolvedLens> lenses =
                lensSetResolver.resolve(null, getLensStyleClasspath(), getPreferredLensPack().getOrElse(""));

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
        return "built-in".equals(sourceLabel) ? "Built-in" : sourceLabel;
    }
}
