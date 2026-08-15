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
 * beyond {@link #stagesFormADependencyChain()} choosing how
 * {@code com.arc_e_tect.gradle.trackerlens.dashboard.TrackerViewFactory} computes each metric
 * card's count - both otherwise only ever operate on {@link TrackerSource} and
 * {@code com.arc_e_tect.gradle.trackerlens.tracker.LifecycleRecord}.</p>
 */
public enum TrackerSourceKind {

    /** Gherkin scenario progress history, stages {@code listed}, {@code defined}, {@code implemented}. */
    GHERKIN_SCENARIO(List.of("listed", "defined", "implemented"), GherkinScenarioTrackerSource::new, true),

    /** API contract progress history, stages {@code declared}, {@code implemented}, {@code verified}. */
    API_CONTRACT(List.of("declared", "implemented", "verified"), ApiContractTrackerSource::new, false);

    private final List<String> stages;
    private final Supplier<TrackerSource> factory;
    private final boolean stagesFormADependencyChain;

    TrackerSourceKind(List<String> stages, Supplier<TrackerSource> factory, boolean stagesFormADependencyChain) {
        this.stages = stages;
        this.factory = factory;
        this.stagesFormADependencyChain = stagesFormADependencyChain;
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
     * Whether an item of this kind can only reach a later stage by having already passed through
     * every earlier one, so its single current (furthest-reached) stage is a meaningful summary of
     * its status.
     *
     * <p>True for {@link #GHERKIN_SCENARIO}: a scenario is implemented only after being defined,
     * and defined only after being listed, in the real authoring workflow - even though the
     * recorded data can skip an intermediate stamp. False for {@link #API_CONTRACT}: declared,
     * implemented, and verified are independent conditions an endpoint may satisfy in any
     * combination (e.g. implemented without being declared), so no single "current stage" captures
     * its status - the dashboard's built-in metric cards use cumulative "reached at least this
     * stage" counts instead.</p>
     *
     * @return {@code true} if this kind's stages form a dependency chain
     */
    public boolean stagesFormADependencyChain() {
        return stagesFormADependencyChain;
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
