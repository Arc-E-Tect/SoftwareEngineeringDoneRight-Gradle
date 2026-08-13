package com.arc_e_tect.gradle.trackerlens;

import com.arc_e_tect.gradle.trackerlens.tracker.ApiContractTrackerSource;
import com.arc_e_tect.gradle.trackerlens.tracker.GherkinScenarioTrackerSource;
import com.arc_e_tect.gradle.trackerlens.tracker.TrackerSource;

import java.util.List;
import java.util.function.Supplier;

/**
 * The built-in {@link TrackerSource} kinds selectable from the {@code trackerLens} DSL's
 * {@code source} property, each paired with its canonical, ordered stage sequence.
 *
 * <p>A third {@link TrackerSource} implementation can be added later purely by adding a constant
 * here (or, if it should never be DSL-selectable, by using it outside this enum entirely) - nothing
 * in {@link GenerateTrackerLensTask} or the dashboard renderer is specific to either built-in kind,
 * since both only ever operate on {@link TrackerSource} and
 * {@code com.arc_e_tect.gradle.trackerlens.tracker.LifecycleRecord}.</p>
 */
public enum TrackerSourceKind {

    /** Gherkin scenario progress history, stages {@code listed}, {@code defined}, {@code implemented}. */
    GHERKIN_SCENARIO(List.of("listed", "defined", "implemented"), GherkinScenarioTrackerSource::new),

    /** API contract progress history, stages {@code declared}, {@code implemented}, {@code verified}. */
    API_CONTRACT(List.of("declared", "implemented", "verified"), ApiContractTrackerSource::new);

    private final List<String> stages;
    private final Supplier<TrackerSource> factory;

    TrackerSourceKind(List<String> stages, Supplier<TrackerSource> factory) {
        this.stages = stages;
        this.factory = factory;
    }

    /**
     * This kind's canonical stage sequence, in order.
     *
     * @return the ordered stage names
     */
    public List<String> stages() {
        return stages;
    }

    /**
     * The stage that marks an item as complete, for {@link com.arc_e_tect.gradle.trackerlens.projection.ProgressProjector}.
     *
     * @return the final stage name
     */
    public String finalStage() {
        return stages.get(stages.size() - 1);
    }

    /**
     * Creates a new {@link TrackerSource} instance of this kind.
     *
     * @return a new tracker source
     */
    public TrackerSource createSource() {
        return factory.get();
    }
}
