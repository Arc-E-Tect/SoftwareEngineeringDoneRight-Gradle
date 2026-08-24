package com.arc_e_tect.gradle.trackerlens;

import com.arc_e_tect.gradle.trackerlens.contract.LensContractValidator;
import com.arc_e_tect.gradle.trackerlens.contract.Violation;
import com.arc_e_tect.gradle.trackerlens.dashboard.DashboardHtmlWriter;
import com.arc_e_tect.gradle.trackerlens.dashboard.DashboardView;
import com.arc_e_tect.gradle.trackerlens.dashboard.TrackerView;
import com.arc_e_tect.gradle.trackerlens.dashboard.TrackerViewFactory;
import com.arc_e_tect.gradle.trackerlens.lens.LensSetResolver;
import com.arc_e_tect.gradle.trackerlens.lens.ResolvedLens;
import com.arc_e_tect.gradle.trackerlens.lens.ResolvedTemplate;
import com.arc_e_tect.gradle.trackerlens.lens.TemplateSetResolver;
import com.arc_e_tect.gradle.trackerlens.projection.Projection;
import com.arc_e_tect.gradle.trackerlens.projection.ProgressProjector;
import com.arc_e_tect.gradle.trackerlens.tracker.HistoryFileResolver;
import com.arc_e_tect.gradle.trackerlens.tracker.LifecycleRecord;
import com.arc_e_tect.gradle.trackerlens.tracker.LifecycleRecordMerger;
import com.arc_e_tect.gradle.trackerlens.tracker.ResponseCoverageCell;
import com.arc_e_tect.gradle.trackerlens.tracker.ResponseCoverageMatrixReader;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads every registered tracker's NDJSON history, computes a completion {@link Projection} for
 * each, resolves the merged lens set, writes {@code dashboard.html} plus one CSS file per
 * discovered lens, and validates the result against {@code ContractRule} as its unconditional last
 * step - a dashboard that silently can't take a lens because generation drifted from its own
 * content contract is a worse failure than a build error.
 */
@DisableCachingByDefault(because = "Generates a dashboard from tracker history files and resolved style packs")
public abstract class GenerateTrackerLensTask extends DefaultTask {

    private final LensSetResolver lensSetResolver = new LensSetResolver();
    private final TemplateSetResolver templateSetResolver = new TemplateSetResolver();
    private final TrackerViewFactory trackerViewFactory = new TrackerViewFactory();
    private final DashboardHtmlWriter dashboardHtmlWriter = new DashboardHtmlWriter();
    private final ProgressProjector progressProjector = new ProgressProjector();
    private final LensContractValidator contractValidator = new LensContractValidator();
    private final HistoryFileResolver historyFileResolver = new HistoryFileResolver();
    private final LifecycleRecordMerger recordMerger = new LifecycleRecordMerger();
    private final ResponseCoverageMatrixReader responseCoverageMatrixReader = new ResponseCoverageMatrixReader();

    /** Creates a new task instance. Instantiated by Gradle infrastructure. */
    @Inject
    public GenerateTrackerLensTask() {
        setGroup("reporting");
        setDescription("Generates the Tracker Lens dashboard from registered tracker history and resolved style packs.");
    }

    /**
     * The resolved tracker registrations to read, as captured from the {@code trackerLens} DSL.
     *
     * @return mutable list property of resolved tracker registrations
     */
    @Internal
    public abstract ListProperty<TrackerSpec> getTrackerSpecs();

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

    /**
     * Optional lens id active on first load.
     *
     * @return mutable property for the default lens id
     */
    @Input
    @Optional
    public abstract Property<String> getDefaultLens();

    /**
     * Where {@code dashboard.html} and its lens CSS files are written.
     *
     * @return mutable directory property for the output directory
     */
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    /**
     * Optional user-supplied Mustache template overriding this plugin's own bundled default for
     * {@code dashboard.html}'s static structure and wording.
     *
     * @return mutable file property for the custom dashboard template
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    @Optional
    public abstract RegularFileProperty getTemplate();

    /**
     * Optional id of a lens-pack-provided template to render {@code dashboard.html} from, instead
     * of this plugin's own bundled default. Mutually exclusive with {@link #getTemplate()}.
     *
     * @return mutable property for the selected lens-pack template's id
     */
    @Input
    @Optional
    public abstract Property<String> getTemplateId();

    /**
     * The dashboard's displayed name, shown in the browser tab title and the page heading. Always
     * present by the time this task runs - {@code TrackerLensExtension} gives it a
     * {@code "<project.name> Lens"} convention, so there is nothing for this property to fall back
     * to on its own.
     *
     * @return mutable property for the dashboard's displayed name
     */
    @Input
    public abstract Property<String> getDashboardName();

    /**
     * Version shown alongside the dashboard's name. Always present by the time this task runs -
     * {@code TrackerLensExtension} gives it a {@code project.version} convention, so there is
     * nothing for this property to fall back to on its own.
     *
     * @return mutable property for the displayed version
     */
    @Input
    public abstract Property<String> getVersion();

    /**
     * Generates the dashboard: reads every registered tracker, resolves lenses, writes the output,
     * and validates it against {@code ContractRule}.
     */
    @TaskAction
    public void generate() {
        if (getTrackerSpecs().get().isEmpty()) {
            // Enforced here, at task-execution time, rather than eagerly for the whole project in
            // TrackerLensPlugin's afterEvaluate: that would fail project configuration - and
            // therefore every task, including the ones that need no tracker at all - the moment
            // zero trackers are registered, rather than failing only when this task itself runs.
            throw new GradleException(
                    "trackerLens: at least one tracker must be registered under trackers { register(\"...\") { ... } }");
        }

        Instant now = Instant.now();
        List<TrackerView> trackerViews = buildTrackerViews(now);
        List<ResolvedLens> lenses = resolveLenses();
        String defaultLensId = resolveDefaultLensId(lenses);

        DashboardView view = new DashboardView(
                trackerViews, lenses, defaultLensId, getDashboardName().get(), getVersion().get());
        File dashboardFile = writeDashboard(view);

        List<Violation> violations = contractValidator.validate(dashboardFile);
        if (!violations.isEmpty()) {
            String details = violations.stream().map(Violation::toString).reduce((a, b) -> a + "; " + b).orElse("");
            throw new GradleException("trackerLens: generated dashboard violates its own content contract: " + details);
        }
    }

    private List<TrackerView> buildTrackerViews(Instant now) {
        List<TrackerView> views = new ArrayList<>();
        for (TrackerSpec spec : getTrackerSpecs().get()) {
            List<LifecycleRecord> records = readAll(spec);
            List<LifecycleRecord> merged = recordMerger.merge(spec.kind().stages(), records);
            int totalCount = (int) merged.stream().filter(record -> record.removedAt() == null).count();
            java.util.Optional<Projection> projection =
                    progressProjector.project(merged, spec.kind().finalStage(), totalCount, now);
            TrackerView view = trackerViewFactory.build(spec.id(), spec.kind().stages(), merged, projection, now,
                    spec.kind().stagesFormADependencyChain());
            if (spec.kind() == TrackerSourceKind.RESPONSE_COVERAGE) {
                view = view.withMatrix(readResponseCoverageMatrix(spec));
            }
            views.add(view);
        }
        return views;
    }

    /**
     * Re-reads every one of {@code spec}'s history files into {@link ResponseCoverageCell}s -
     * the raw current-state snapshot a coverage-depth grid needs, which the milestone-shaped
     * {@link LifecycleRecord}s {@link #readAll(TrackerSpec)} already produced cannot carry (no
     * {@code testCount} field). A missing configured entry is silently skipped here exactly like
     * {@link #readAll(TrackerSpec)} already does, for the same reason - the producing plugin may
     * simply not have run yet.
     */
    private List<ResponseCoverageCell> readResponseCoverageMatrix(TrackerSpec spec) {
        List<ResponseCoverageCell> cells = new ArrayList<>();
        for (File file : historyFileResolver.resolve(spec.historyFiles())) {
            if (!file.isFile()) {
                continue;
            }
            cells.addAll(responseCoverageMatrixReader.read(file));
        }
        return cells;
    }

    private List<LifecycleRecord> readAll(TrackerSpec spec) {
        List<LifecycleRecord> records = new ArrayList<>();
        for (File file : historyFileResolver.resolve(spec.historyFiles())) {
            if (!file.isFile()) {
                // Rendered as a tracker section with no data yet, rather than omitted outright, so a
                // producing plugin that simply hasn't run yet never leaves the dashboard itself in
                // contract violation (every registered tracker still gets its TRACKER_SECTION).
                getLogger().lifecycle("trackerLens: tracker '{}' history file does not exist yet, skipping: {}",
                        spec.id(), file);
                continue;
            }
            records.addAll(spec.kind().createSource().read(file));
        }
        return records;
    }

    private File writeDashboard(DashboardView view) {
        boolean templateFileSet = getTemplate().isPresent();
        boolean templateIdSet = getTemplateId().isPresent() && !getTemplateId().get().isBlank();
        if (templateFileSet && templateIdSet) {
            throw new GradleException("trackerLens: template and templateId are mutually exclusive - "
                    + "set at most one of them");
        }

        File outputDirectory = getOutputDirectory().get().getAsFile();
        if (templateIdSet) {
            String id = getTemplateId().get();
            List<ResolvedTemplate> templates = resolveTemplates();
            ResolvedTemplate template = templates.stream()
                    .filter(candidate -> candidate.id().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new GradleException("trackerLens: no template with id '" + id
                            + "' found among the resolved lens packs. Available: "
                            + templates.stream().map(ResolvedTemplate::id).toList()));
            return dashboardHtmlWriter.write(outputDirectory, view, template.id(),
                    new String(template.content(), StandardCharsets.UTF_8));
        }

        File customTemplate = templateFileSet ? getTemplate().get().getAsFile() : null;
        return dashboardHtmlWriter.write(outputDirectory, view, customTemplate);
    }

    private List<ResolvedTemplate> resolveTemplates() {
        return templateSetResolver.resolve(getLensStyleClasspath(), getPreferredLensPack().getOrElse(""));
    }

    private List<ResolvedLens> resolveLenses() {
        File lensStylesheet = getLensStylesheet().isPresent() ? getLensStylesheet().get().getAsFile() : null;
        return lensSetResolver.resolve(lensStylesheet, getLensStyleClasspath(), getPreferredLensPack().getOrElse(""));
    }

    String resolveDefaultLensId(List<ResolvedLens> lenses) {
        if (getDefaultLens().isPresent() && !getDefaultLens().get().isBlank()) {
            return getDefaultLens().get();
        }
        return lenses.stream().map(ResolvedLens::id).filter(id -> id.equals("light-lens")).findFirst()
                .orElseGet(() -> lenses.stream().map(ResolvedLens::id).sorted().findFirst()
                        .orElseThrow(() -> new GradleException("trackerLens: no lenses were discovered")));
    }
}
