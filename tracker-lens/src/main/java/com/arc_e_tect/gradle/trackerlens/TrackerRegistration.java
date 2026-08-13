package com.arc_e_tect.gradle.trackerlens;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

/**
 * One {@code trackerLens.trackers { register("...") { ... } }} DSL registration: which history
 * file(s) to read and which {@link TrackerSourceKind} to read them with.
 *
 * <pre>
 * trackerLens {
 *     trackers {
 *         register("bdd-scenarios") {
 *             historyFiles.from(file("gherkin-progress-history.ndjson"))
 *             source = TrackerSourceKind.GHERKIN_SCENARIO
 *         }
 *         register("api-contracts") {
 *             // One history file per detector plugin, or a directory containing them - either
 *             // way, records sharing the same id across files are merged into one.
 *             historyFiles.from(
 *                 file("shadow-api-detector-contract-history.ndjson"),
 *                 file("mirage-api-detector-contract-history.ndjson"),
 *                 file("doppelganger-api-detector-contract-history.ndjson"))
 *             source = TrackerSourceKind.API_CONTRACT
 *         }
 *     }
 * }
 * </pre>
 */
public abstract class TrackerRegistration {

    private final String name;

    /**
     * Creates a new {@code TrackerRegistration}. Instantiated by Gradle's
     * {@code NamedDomainObjectContainer} infrastructure.
     *
     * @param name this tracker's registered name, becomes {@code data-tracker} in the dashboard
     */
    @Inject
    public TrackerRegistration(String name) {
        this.name = name;
    }

    /**
     * This tracker's registered name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * The NDJSON history file(s) to read. Accepts one or more individual files and/or a directory
     * containing them (every {@code *.ndjson} file directly within it, non-recursive) - useful
     * when a tracker's history is split across several files, e.g. one contract history file per
     * API-detector plugin instead of all three sharing one. Records sharing the same id across
     * files are merged into a single item rather than rendered as duplicates.
     *
     * <p>A configured entry that does not exist at task-run time is skipped with a warning, not a
     * build failure - the producing plugin may not have run yet.</p>
     *
     * @return mutable file collection for the history file(s)/directory
     */
    public abstract ConfigurableFileCollection getHistoryFiles();

    /**
     * Which built-in {@link com.arc_e_tect.gradle.trackerlens.tracker.TrackerSource} to read
     * {@link #getHistoryFiles()} with.
     *
     * @return mutable property for the tracker source kind
     */
    public abstract Property<TrackerSourceKind> getSource();
}
