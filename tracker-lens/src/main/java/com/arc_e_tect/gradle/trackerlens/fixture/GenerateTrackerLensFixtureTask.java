package com.arc_e_tect.gradle.trackerlens.fixture;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;

import javax.inject.Inject;
import java.time.Instant;

/**
 * Generates a calibrated pair of {@code gherkin-progress-history.ndjson} and
 * {@code api-contract-progress.ndjson} fixture files - a convenience wrapper around
 * {@link TrackerLensFixtureGenerator} for a lens-pack developer prototyping locally, exposing
 * {@link FixtureSpec}'s fields as task properties, every one of them optional and defaulting
 * exactly as {@link FixtureSpec#defaults()} does.
 *
 * <p>Left entirely at its defaults, {@code asOf} defaults to the current instant at task-execution
 * time - not at configuration time - so re-running this task always regenerates a fixture fresh
 * relative to whenever it's run, exactly like {@link FixtureSpec#defaults()} does for a direct Java
 * caller. {@code Instant.now()} is invisible to Gradle's own input snapshotting - unlike a real
 * {@code @Input} property, an unset one is recorded identically on every execution - so without
 * {@link UntrackedTask @UntrackedTask} a second, otherwise-unchanged invocation would report
 * {@code UP-TO-DATE} and silently skip regenerating, defeating the entire point of leaving
 * {@code asOf} at its default. {@code @UntrackedTask} disables both up-to-date checking and the
 * shared build cache for this task, matching what's actually true of it: every execution can
 * legitimately produce different output that no declared input/output tracking could represent.</p>
 */
@UntrackedTask(because = "asOf defaults to Instant.now() at execution time, invisible to Gradle's input "
        + "snapshotting, so every execution can legitimately produce different output - neither "
        + "up-to-date checking nor the build cache would ever be safe to apply here")
public abstract class GenerateTrackerLensFixtureTask extends DefaultTask {

    private final TrackerLensFixtureGenerator generator = new TrackerLensFixtureGenerator();

    /** Creates a new task instance. Instantiated by Gradle infrastructure. */
    @Inject
    public GenerateTrackerLensFixtureTask() {
        setGroup("tracker lens");
        setDescription("Generates a calibrated pair of tracker-lens NDJSON history fixtures whose forecast lands "
                + "a predictable distance in the future.");
    }

    /**
     * The instant to generate the fixture as of, as an ISO-8601 string (e.g.
     * {@code 2026-08-16T00:00:00Z}). Unset by default, resolving to {@link Instant#now()} at
     * task-execution time.
     *
     * @return mutable property for the {@code asOf} instant
     */
    @Input
    @Optional
    public abstract Property<String> getAsOf();

    /**
     * How many days of history, ending at {@code asOf}, the generated timestamps are spread
     * across. Defaults to {@link FixtureSpec#defaults()}'s value.
     *
     * @return mutable property for {@code historyStartDaysAgo}
     */
    @Input
    @Optional
    public abstract Property<Integer> getHistoryStartDaysAgo();

    /**
     * How many calendar days out from {@code asOf} the calibrated forecast should land. Defaults to
     * {@link FixtureSpec#defaults()}'s value.
     *
     * @return mutable property for {@code forecastTargetDaysOut}
     */
    @Input
    @Optional
    public abstract Property<Integer> getForecastTargetDaysOut();

    /**
     * Must be left at 5 - see {@link FixtureSpec#workingDaysPerWeek()}.
     *
     * @return mutable property for {@code workingDaysPerWeek}
     */
    @Input
    @Optional
    public abstract Property<Integer> getWorkingDaysPerWeek();

    /**
     * How many BDD-scenario records to generate. Defaults to {@link FixtureSpec#defaults()}'s
     * value.
     *
     * @return mutable property for {@code bddScenarioCount}
     */
    @Input
    @Optional
    public abstract Property<Integer> getBddScenarioCount();

    /**
     * How many API-contract records to generate. Defaults to {@link FixtureSpec#defaults()}'s
     * value.
     *
     * @return mutable property for {@code apiContractCount}
     */
    @Input
    @Optional
    public abstract Property<Integer> getApiContractCount();

    /**
     * Where the generated BDD-scenario history is written.
     *
     * @return mutable file property for the BDD-scenario history output file
     */
    @OutputFile
    public abstract RegularFileProperty getBddScenarioHistoryFile();

    /**
     * Where the generated API-contract history is written.
     *
     * @return mutable file property for the API-contract history output file
     */
    @OutputFile
    public abstract RegularFileProperty getApiContractHistoryFile();

    /** Builds a {@link FixtureSpec} from this task's properties and delegates to {@link TrackerLensFixtureGenerator}. */
    @TaskAction
    public void generate() {
        FixtureSpec defaults = FixtureSpec.defaults();
        FixtureSpec spec = new FixtureSpec(
                getAsOf().isPresent() ? Instant.parse(getAsOf().get()) : Instant.now(),
                getHistoryStartDaysAgo().getOrElse(defaults.historyStartDaysAgo()),
                getForecastTargetDaysOut().getOrElse(defaults.forecastTargetDaysOut()),
                getWorkingDaysPerWeek().getOrElse(defaults.workingDaysPerWeek()),
                getBddScenarioCount().getOrElse(defaults.bddScenarioCount()),
                getApiContractCount().getOrElse(defaults.apiContractCount()));

        FixtureResult result = generator.generate(spec);
        result.writeTo(getBddScenarioHistoryFile().get().getAsFile(), getApiContractHistoryFile().get().getAsFile());

        getLogger().lifecycle(
                "trackerLens: wrote a calibrated fixture ({} bdd scenarios, {} api contracts) forecasting "
                + "~{} days out from {}",
                result.bddScenarios().size(), result.apiContracts().size(), spec.forecastTargetDaysOut(), spec.asOf());
    }
}
