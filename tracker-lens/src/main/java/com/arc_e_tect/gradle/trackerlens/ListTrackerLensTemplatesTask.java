package com.arc_e_tect.gradle.trackerlens;

import com.arc_e_tect.gradle.trackerlens.lens.ResolvedTemplate;
import com.arc_e_tect.gradle.trackerlens.lens.TemplateSetResolver;
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
 * Lists every Tracker Lens dashboard template available through a lens pack - this plugin's own
 * built-in template and every resolved external {@code lensStyle} pack's templates - selectable via
 * {@code trackerLens.templateId}, without generating a dashboard.
 *
 * <p>Scoped to lens packs only, the same way {@link ListTrackerLensStylesTask} is scoped to lens
 * packs only: a project's own one-off {@code trackerLens.template} file is a different mechanism
 * entirely and never appears here, since it isn't discoverable by id the way a pack's templates are.
 * Resolves the exact same template set as {@code generateTrackerLensDashboard} would when
 * {@code templateId} is set, via the shared {@link TemplateSetResolver}, so this listing can never
 * drift from what {@code templateId} can actually select. Needs no tracker to be registered at all -
 * it only touches template resolution.</p>
 */
@DisableCachingByDefault(because = "Prints the available dashboard templates to the console; produces no outputs")
public abstract class ListTrackerLensTemplatesTask extends DefaultTask {

    private final TemplateSetResolver templateSetResolver = new TemplateSetResolver();

    /** Creates a new task instance. Instantiated by Gradle infrastructure. */
    @Inject
    public ListTrackerLensTemplatesTask() {
        setGroup("reporting");
        setDescription("Lists every Tracker Lens dashboard template available through a lens pack "
                + "(built-in and external lensStyle packs), selectable via trackerLens.templateId.");
    }

    /**
     * Classpath entries (directories or jars) resolved from the {@code lensStyle} configuration.
     *
     * @return mutable file collection of external style-pack classpath roots
     */
    @Classpath
    public abstract ConfigurableFileCollection getLensStyleClasspath();

    /**
     * Optional coordinate restricting which single external style pack's templates are offered.
     *
     * @return mutable property for the preferred lens pack coordinate
     */
    @Input
    @Optional
    public abstract Property<String> getPreferredLensPack();

    /** Resolves the template set and prints it to the console, grouped by source. */
    @TaskAction
    public void list() {
        List<ResolvedTemplate> templates =
                templateSetResolver.resolve(getLensStyleClasspath(), getPreferredLensPack().getOrElse(""));

        Map<String, List<String>> idsBySource = new LinkedHashMap<>();
        for (ResolvedTemplate template : templates) {
            idsBySource.computeIfAbsent(template.sourceLabel(), key -> new ArrayList<>()).add(template.id());
        }

        System.out.println("Tracker Lens templates available (" + templates.size() + "):");
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
