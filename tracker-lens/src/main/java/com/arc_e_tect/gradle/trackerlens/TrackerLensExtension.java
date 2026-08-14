package com.arc_e_tect.gradle.trackerlens;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

/**
 * DSL extension for the Tracker Lens Gradle plugin.
 *
 * <pre>
 * trackerLens {
 *     trackers {
 *         register("bdd-scenarios") { historyFiles.from(file("gherkin-progress-history.ndjson")); source = TrackerSourceKind.GHERKIN_SCENARIO }
 *         register("api-contracts") { historyFiles.from(file("api-contract-progress.ndjson")); source = TrackerSourceKind.API_CONTRACT }
 *     }
 *     lensStylesheet = file("my-theme.css")           // optional: contributes a lens called "custom-lens", highest precedence for that one id
 *     preferredLensPack = "com.example:midnight-theme" // optional: restrict which external pack's lenses are offered
 *     defaultLens = "dark-lens"                        // optional
 *     template = file("dashboard-template.html")       // optional: override the bundled dashboard.html Mustache template
 *     outputDir = layout.buildDirectory.dir("reports/tracker-lens")  // default shown
 *     dashboardName = "Checkout Service Lens"          // optional; default: "${project.name} Lens"
 *     version = "2.3.0"                                // optional; default: project.version
 * }
 *
 * dependencies {
 *     lensStyle("com.example:midnight-theme:1.2.0")
 * }
 * </pre>
 */
public abstract class TrackerLensExtension {

    /** Extension DSL block name, i.e. the name used to register the extension with the project. */
    public static final String NAME = "trackerLens";

    /** Name of the resolvable configuration external style packs are declared against. */
    public static final String LENS_STYLE_CONFIGURATION_NAME = "lensStyle";

    private final NamedDomainObjectContainer<TrackerRegistration> trackers;

    /**
     * Creates the extension. Instantiated by Gradle's extension-creation infrastructure.
     *
     * @param objects Gradle's object factory
     */
    @Inject
    public TrackerLensExtension(ObjectFactory objects) {
        trackers = objects.domainObjectContainer(TrackerRegistration.class,
                name -> objects.newInstance(TrackerRegistration.class, name));
    }

    /**
     * The registered trackers. At least one must be registered.
     *
     * @return the tracker registration container
     */
    public NamedDomainObjectContainer<TrackerRegistration> getTrackers() {
        return trackers;
    }

    /**
     * Configures {@link #getTrackers()}.
     *
     * @param action the configuration block
     */
    public void trackers(Action<? super NamedDomainObjectContainer<TrackerRegistration>> action) {
        action.execute(trackers);
    }

    /**
     * Optional single CSS file contributing a lens called {@code custom-lens}, always resolved with
     * the highest precedence for that id (it is placed first among lens sources, so it can never be
     * pushed into an auto-namespaced id by a collision).
     *
     * @return mutable file property for the custom lens stylesheet
     */
    public abstract RegularFileProperty getLensStylesheet();

    /**
     * Optional Maven coordinate restricting which single external {@code lensStyle} pack's lenses
     * are offered in the switcher, for when several resolved packs contribute distinct,
     * non-colliding lenses and only one pack's lenses should ever be shown. Not needed to resolve an
     * id collision - those are auto-namespaced regardless of this property.
     *
     * @return mutable property for the preferred lens pack coordinate
     */
    public abstract Property<String> getPreferredLensPack();

    /**
     * The lens id active on first load. When unset, defaults to {@code light-lens} if discovered,
     * otherwise the first lens id in alphabetical order.
     *
     * @return mutable property for the default lens id
     */
    public abstract Property<String> getDefaultLens();

    /**
     * Where {@code dashboard.html} and its lens CSS files are written.
     *
     * @return mutable directory property for the output directory
     */
    public abstract DirectoryProperty getOutputDir();

    /**
     * Optional Mustache template overriding this plugin's own bundled default for
     * {@code dashboard.html}. Every data-bound element {@code ContractRule} governs is still
     * available to the template as context (see {@code DASHBOARD-THEMING.adoc}); everything else -
     * headings, captions, disclaimer wording - is the template's own literal text, so a translated
     * or reworded copy of the bundled template survives regeneration instead of being overwritten.
     * Whichever template rendered it, the output is always validated against {@code ContractRule}
     * exactly like the bundled default's own output is.
     *
     * @return mutable file property for the custom dashboard template
     */
    public abstract RegularFileProperty getTemplate();

    /**
     * The dashboard's displayed name, shown in the browser tab title and the page heading.
     * Defaults to {@code "<project.name> Lens"}; set this to use a different name verbatim -
     * whatever it's set to is used exactly as given, with no {@code " Lens"} suffix added.
     *
     * @return mutable property for the dashboard's displayed name
     */
    public abstract Property<String> getDashboardName();

    /**
     * Version shown alongside the dashboard's name, the same way {@code systemUnderTestVersion}
     * works on the API-detector plugins (e.g. {@code shadow-api-detector}): defaults to the
     * project's own {@code version} (as set in the build file or a properties file); set this
     * property to show a different version instead, e.g. when the trackers registered here reflect
     * a different artifact than the one being built.
     *
     * @return mutable property for the displayed version
     */
    public abstract Property<String> getVersion();
}
