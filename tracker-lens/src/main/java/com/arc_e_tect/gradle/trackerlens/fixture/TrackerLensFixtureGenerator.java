package com.arc_e_tect.gradle.trackerlens.fixture;

import com.arc_e_tect.gradle.trackerlens.TrackerSourceKind;
import com.arc_e_tect.gradle.trackerlens.projection.Projection;
import com.arc_e_tect.gradle.trackerlens.projection.ProgressProjector;
import com.arc_e_tect.gradle.trackerlens.tracker.LifecycleRecord;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Generates a calibrated pair of {@code gherkin-progress-history.ndjson} and
 * {@code api-contract-progress.ndjson} fixtures: run today, the forecast each one produces lands a
 * predictable distance in the future, and still does when run again months later, without anyone
 * hand-editing timestamps.
 *
 * <h2>Calibration, not a formula</h2>
 *
 * <p>{@link com.arc_e_tect.gradle.trackerlens.projection.ProgressProjector} has a specific, adaptive
 * shape - a lookback window capped at 90 days, a 7-day floor below which it refuses to forecast, a
 * particular velocity computation - that this class deliberately does not re-derive as a closed-form
 * equation. Instead, {@link #generate(FixtureSpec)} builds a candidate dataset, runs it through a
 * real {@link ProgressProjector} instance, reads the resulting forecast date, and searches (by
 * varying how many items have already reached each tracker's final stage, and how their timestamps
 * are spread across the lookback window) until the computed forecast lands within a day or two of
 * {@link FixtureSpec#forecastTargetDaysOut()}. This is what keeps the fixture correct automatically
 * if {@code ProgressProjector}'s internals ever change - only this search loop's tolerance and
 * iteration bounds are this class's own concern, never the projector's math.</p>
 *
 * <h2>Default coverage</h2>
 *
 * <p>{@link FixtureSpec#defaults()} always yields, on both trackers, at least one record with
 * {@code removedAt} set. The API-contract tracker always includes at least one record in each of
 * the five categories a lens typically classifies contracts by - Shadow (implemented, never
 * declared), Mirage (declared, never implemented), Stubbed (a consumer-facing stub exists,
 * independent of implementation or verification status), Doppelganger (declared and implemented,
 * never verified), and Compliant (declared, implemented, and verified) - with a dedicated record
 * that is simultaneously Compliant and still has {@code stubbedAt} set, proving stub status is
 * irrelevant once real implementation and verification are both present. The BDD-scenario tracker
 * always includes a representative at each of {@code listed}, {@code defined}, and
 * {@code implemented}, plus a record whose {@code definedAt} is never set despite
 * {@code implementedAt} being set - the stage-skip case a naive furthest-in-list-order reducer gets
 * wrong. Requesting more than the default count scales every category proportionally rather than
 * padding a fixed handful of them.</p>
 */
public final class TrackerLensFixtureGenerator {

    private static final int TOLERANCE_DAYS = 2;

    private static final int API_MANDATORY_ROWS = 6;
    private static final int BDD_MANDATORY_ROWS = 5;

    private static final String[] VERBS = {"GET", "POST", "PUT", "DELETE", "PATCH"};
    private static final String[] API_RESOURCES = {
            "orders", "customers", "invoices", "shipments", "products", "payments", "subscriptions",
            "notifications", "reports", "sessions", "accounts", "carts", "reviews", "coupons",
            "warehouses", "returns", "wishlists", "support-tickets", "analytics", "promotions"
    };
    private static final String[][] BDD_SCENARIOS = {
            {"Ordering", "Place an order"},
            {"Ordering", "Cancel an order"},
            {"Ordering", "Refund an order"},
            {"Shipping", "Track a shipment"},
            {"Shipping", "Split a shipment"},
            {"Promotions", "Apply a discount code"},
            {"Checkout", "Apply a coupon"},
            {"Support", "Raise a support ticket"},
            {"Accounts", "Reset a password"},
            {"Wishlists", "Add an item to a wishlist"},
    };

    private final ProgressProjector progressProjector = new ProgressProjector();

    /** Creates a new {@code TrackerLensFixtureGenerator}. */
    public TrackerLensFixtureGenerator() {}

    /**
     * Generates a calibrated fixture for {@code spec}.
     *
     * @param spec the generation parameters
     * @return the generated records for both trackers, ready to inspect in memory or write to disk
     * @throws FixtureCalibrationException if no candidate dataset tried against the real
     *         {@link ProgressProjector} landed within tolerance of
     *         {@link FixtureSpec#forecastTargetDaysOut()} - see that exception's Javadoc for when
     *         this is expected, e.g. a target too aggressive for {@code historyStartDaysAgo}
     */
    public FixtureResult generate(FixtureSpec spec) {
        List<BddScenarioFixtureRecord> bddScenarios = buildBddScenarios(spec);
        List<ApiContractFixtureRecord> apiContracts = buildApiContracts(spec);
        return new FixtureResult(bddScenarios, apiContracts);
    }

    // -------------------------------------------------------------------------------------------
    // API contracts
    // -------------------------------------------------------------------------------------------

    private List<ApiContractFixtureRecord> buildApiContracts(FixtureSpec spec) {
        int totalRows = Math.max(spec.apiContractCount(), API_MANDATORY_ROWS);
        int extra = totalRows - API_MANDATORY_ROWS;
        int minDone = 1;
        int maxDone = 1 + extra;

        Calibration calibration = calibrate(spec, minDone, maxDone, TrackerSourceKind.API_CONTRACT.finalStage(),
                totalRows - 1, (doneCount, earliestOffsetDays) ->
                        apiRecordsToLifecycleRecords(buildApiRecords(spec, totalRows, doneCount, earliestOffsetDays)));

        return buildApiRecords(spec, totalRows, calibration.doneCount(), calibration.earliestOffsetDays());
    }

    private List<ApiContractFixtureRecord> buildApiRecords(
            FixtureSpec spec, int totalRows, int doneCount, int earliestOffsetDays) {
        Instant asOf = spec.asOf();
        Instant windowStart = asOf.minus(Duration.ofDays(spec.historyStartDaysAgo()));
        Instant doneEarliest = asOf.minus(Duration.ofDays(earliestOffsetDays));
        List<Instant> doneTimestamps = spreadTimestamps(doneCount, doneEarliest, asOf);

        List<ApiContractFixtureRecord> records = new ArrayList<>();
        int doneIndex = 0;
        int row = 0;

        // Mandatory coverage: one representative per category, regardless of requested count. The
        // "mirage" row is the one that also demonstrates the Stubbed axis (a consumer-facing stub
        // for a declared-but-not-yet-implemented endpoint), independent of the other four.
        records.add(compliantApiRecord(row++, windowStart, doneTimestamps.get(doneIndex++), true));
        records.add(notDoneApiRecord(row++, "shadow", false, windowStart, asOf));
        records.add(notDoneApiRecord(row++, "mirage", true, windowStart, asOf));
        records.add(notDoneApiRecord(row++, "mirage", false, windowStart, asOf));
        records.add(notDoneApiRecord(row++, "doppelganger", false, windowStart, asOf));
        records.add(removedApiRecord(row++, windowStart, asOf));

        int extra = totalRows - API_MANDATORY_ROWS;
        int extraDone = doneCount - 1;
        String[] notDoneCategories = {"shadow", "mirage", "doppelganger"};
        for (int i = 0; i < extra; i++) {
            if (i < extraDone) {
                records.add(compliantApiRecord(row++, windowStart, doneTimestamps.get(doneIndex++), i % 3 == 0));
            } else {
                int notDoneIndex = i - extraDone;
                String category = notDoneCategories[notDoneIndex % notDoneCategories.length];
                // The Stubbed axis only ever applies to a declared endpoint, so it scales in step
                // with the requested count across "mirage" and "doppelganger" rows, never "shadow".
                boolean stubbed = !"shadow".equals(category) && notDoneIndex % 3 == 1;
                records.add(notDoneApiRecord(row++, category, stubbed, windowStart, asOf));
            }
        }
        return records;
    }

    private ApiContractFixtureRecord compliantApiRecord(int row, Instant windowStart, Instant verifiedAt, boolean stubbed) {
        ApiIdentity identity = apiIdentity(row);
        Instant declaredAt = between(windowStart, verifiedAt, 0.1);
        Instant implementedAt = between(windowStart, verifiedAt, 0.4);
        Instant stubbedAt = stubbed ? between(windowStart, verifiedAt, 0.25) : null;
        return new ApiContractFixtureRecord(identity.fingerprint(), identity.verb(), identity.path(),
                declaringClass(identity), declaredAt, implementedAt, stubbedAt, verifiedAt, verifiedAt, null);
    }

    private ApiContractFixtureRecord notDoneApiRecord(int row, String category, boolean stubbed, Instant windowStart, Instant asOf) {
        ApiIdentity identity = apiIdentity(row);
        Instant declaredAt = null;
        Instant implementedAt = null;
        Instant stubbedAt = null;
        switch (category) {
            case "shadow" -> implementedAt = between(windowStart, asOf, 0.3);
            case "mirage" -> {
                declaredAt = between(windowStart, asOf, 0.2);
                stubbedAt = stubbed ? between(windowStart, asOf, 0.35) : null;
            }
            case "doppelganger" -> {
                declaredAt = between(windowStart, asOf, 0.15);
                implementedAt = between(windowStart, asOf, 0.5);
                stubbedAt = stubbed ? between(windowStart, asOf, 0.25) : null;
            }
            default -> throw new IllegalArgumentException("trackerLensFixture: unknown API category: " + category);
        }
        Instant lastSeenAt = asOf;
        return new ApiContractFixtureRecord(identity.fingerprint(), identity.verb(), identity.path(),
                declaringClass(identity, implementedAt != null), declaredAt, implementedAt, stubbedAt, null, lastSeenAt, null);
    }

    private ApiContractFixtureRecord removedApiRecord(int row, Instant windowStart, Instant asOf) {
        ApiIdentity identity = apiIdentity(row);
        Instant declaredAt = between(windowStart, asOf, 0.1);
        Instant removedAt = between(windowStart, asOf, 0.85);
        Instant lastSeenAt = between(windowStart, asOf, 0.8);
        return new ApiContractFixtureRecord(identity.fingerprint(), identity.verb(), identity.path(),
                null, declaredAt, null, null, null, lastSeenAt, removedAt);
    }

    private String declaringClass(ApiIdentity identity) {
        return declaringClass(identity, true);
    }

    private String declaringClass(ApiIdentity identity, boolean implemented) {
        if (!implemented) {
            return null;
        }
        String[] segments = identity.resource().split("-");
        StringBuilder className = new StringBuilder("com.example.");
        for (String segment : segments) {
            className.append(Character.toUpperCase(segment.charAt(0))).append(segment.substring(1));
        }
        className.append("Controller");
        return className.toString();
    }

    private ApiIdentity apiIdentity(int row) {
        String verb = VERBS[row % VERBS.length];
        String resource = API_RESOURCES[row % API_RESOURCES.length];
        int cycle = row / API_RESOURCES.length;
        String suffix = cycle > 0 ? "-" + cycle : "";
        String path = "/api/" + resource + suffix;
        String fingerprint = verb.toLowerCase(Locale.ROOT) + "-" + resource + suffix;
        return new ApiIdentity(verb, path, resource, fingerprint);
    }

    private record ApiIdentity(String verb, String path, String resource, String fingerprint) {
    }

    private List<LifecycleRecord> apiRecordsToLifecycleRecords(List<ApiContractFixtureRecord> records) {
        List<LifecycleRecord> result = new ArrayList<>();
        for (ApiContractFixtureRecord record : records) {
            Map<String, Instant> stages = new LinkedHashMap<>();
            putIfPresent(stages, "declared", record.declaredAt());
            putIfPresent(stages, "implemented", record.implementedAt());
            putIfPresent(stages, "stubbed", record.stubbedAt());
            putIfPresent(stages, "verified", record.verifiedAt());
            result.add(new LifecycleRecord(record.fingerprint(), record.verb() + " " + record.path(),
                    record.declaringClass(), stages, record.lastSeenAt(), record.removedAt()));
        }
        return result;
    }

    // -------------------------------------------------------------------------------------------
    // BDD scenarios
    // -------------------------------------------------------------------------------------------

    private List<BddScenarioFixtureRecord> buildBddScenarios(FixtureSpec spec) {
        int totalRows = Math.max(spec.bddScenarioCount(), BDD_MANDATORY_ROWS);
        int extra = totalRows - BDD_MANDATORY_ROWS;
        int minDone = 2;
        int maxDone = 2 + extra;

        Calibration calibration = calibrate(spec, minDone, maxDone, TrackerSourceKind.GHERKIN_SCENARIO.finalStage(),
                totalRows - 1, (doneCount, earliestOffsetDays) ->
                        bddRecordsToLifecycleRecords(buildBddRecords(spec, totalRows, doneCount, earliestOffsetDays)));

        return buildBddRecords(spec, totalRows, calibration.doneCount(), calibration.earliestOffsetDays());
    }

    private List<BddScenarioFixtureRecord> buildBddRecords(
            FixtureSpec spec, int totalRows, int doneCount, int earliestOffsetDays) {
        Instant asOf = spec.asOf();
        Instant windowStart = asOf.minus(Duration.ofDays(spec.historyStartDaysAgo()));
        Instant doneEarliest = asOf.minus(Duration.ofDays(earliestOffsetDays));
        List<Instant> doneTimestamps = spreadTimestamps(doneCount, doneEarliest, asOf);

        List<BddScenarioFixtureRecord> records = new ArrayList<>();
        int doneIndex = 0;
        int row = 0;

        // Mandatory coverage: a representative at each stage, plus the stage-skip case.
        records.add(implementedBddRecord(row++, windowStart, doneTimestamps.get(doneIndex++), true));
        records.add(implementedBddRecord(row++, windowStart, doneTimestamps.get(doneIndex++), false));
        records.add(notDoneBddRecord(row++, "listed", windowStart, asOf));
        records.add(notDoneBddRecord(row++, "defined", windowStart, asOf));
        records.add(removedBddRecord(row++, windowStart, asOf));

        int extra = totalRows - BDD_MANDATORY_ROWS;
        int extraDone = doneCount - 2;
        String[] notDoneStages = {"listed", "defined"};
        for (int i = 0; i < extra; i++) {
            if (i < extraDone) {
                records.add(implementedBddRecord(row++, windowStart, doneTimestamps.get(doneIndex++), true));
            } else {
                records.add(notDoneBddRecord(row++, notDoneStages[i % notDoneStages.length], windowStart, asOf));
            }
        }
        return records;
    }

    private BddScenarioFixtureRecord implementedBddRecord(int row, Instant windowStart, Instant implementedAt, boolean withDefined) {
        BddIdentity identity = bddIdentity(row);
        Instant listedAt = between(windowStart, implementedAt, 0.1);
        Instant definedAt = withDefined ? between(windowStart, implementedAt, 0.4) : null;
        return new BddScenarioFixtureRecord(identity.fingerprint(), identity.scenarioName(), identity.featureTitle(),
                listedAt, definedAt, implementedAt, implementedAt, null);
    }

    private BddScenarioFixtureRecord notDoneBddRecord(int row, String stage, Instant windowStart, Instant asOf) {
        BddIdentity identity = bddIdentity(row);
        Instant listedAt = between(windowStart, asOf, 0.2);
        Instant definedAt = "defined".equals(stage) ? between(windowStart, asOf, 0.5) : null;
        Instant lastSeenAt = asOf;
        return new BddScenarioFixtureRecord(identity.fingerprint(), identity.scenarioName(), identity.featureTitle(),
                listedAt, definedAt, null, lastSeenAt, null);
    }

    private BddScenarioFixtureRecord removedBddRecord(int row, Instant windowStart, Instant asOf) {
        BddIdentity identity = bddIdentity(row);
        Instant listedAt = between(windowStart, asOf, 0.1);
        Instant removedAt = between(windowStart, asOf, 0.85);
        Instant lastSeenAt = between(windowStart, asOf, 0.8);
        return new BddScenarioFixtureRecord(identity.fingerprint(), identity.scenarioName(), identity.featureTitle(),
                listedAt, null, null, lastSeenAt, removedAt);
    }

    private BddIdentity bddIdentity(int row) {
        int cycle = row / BDD_SCENARIOS.length;
        String[] entry = BDD_SCENARIOS[row % BDD_SCENARIOS.length];
        String featureTitle = entry[0];
        String scenarioName = cycle > 0 ? entry[1] + " (" + (cycle + 1) + ")" : entry[1];
        String slug = scenarioName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return new BddIdentity(slug, scenarioName, featureTitle);
    }

    private record BddIdentity(String fingerprint, String scenarioName, String featureTitle) {
    }

    private List<LifecycleRecord> bddRecordsToLifecycleRecords(List<BddScenarioFixtureRecord> records) {
        List<LifecycleRecord> result = new ArrayList<>();
        for (BddScenarioFixtureRecord record : records) {
            Map<String, Instant> stages = new LinkedHashMap<>();
            putIfPresent(stages, "listed", record.listedAt());
            putIfPresent(stages, "defined", record.definedAt());
            putIfPresent(stages, "implemented", record.implementedAt());
            result.add(new LifecycleRecord(record.fingerprint(), record.scenarioName(), record.featureTitle(),
                    stages, record.lastSeenAt(), record.removedAt()));
        }
        return result;
    }

    // -------------------------------------------------------------------------------------------
    // Calibration
    // -------------------------------------------------------------------------------------------

    private void putIfPresent(Map<String, Instant> stages, String stage, Instant value) {
        if (value != null) {
            stages.put(stage, value);
        }
    }

    private Instant between(Instant start, Instant end, double fraction) {
        Duration span = Duration.between(start, end);
        if (span.isNegative() || span.isZero()) {
            return start;
        }
        return start.plus(Duration.ofMillis(Math.round(span.toMillis() * fraction)));
    }

    private List<Instant> spreadTimestamps(int count, Instant start, Instant end) {
        List<Instant> result = new ArrayList<>(count);
        if (count == 1) {
            result.add(start);
            return result;
        }
        Duration span = Duration.between(start, end);
        for (int i = 0; i < count; i++) {
            double fraction = (double) i / (count - 1);
            result.add(start.plus(Duration.ofMillis(Math.round(span.toMillis() * fraction))));
        }
        return result;
    }

    /**
     * How far past {@code asOf} candidates are verified across - modeling the real gap between
     * {@code generateTrackerLensFixture} writing the fixture and a separate, later
     * {@code generateTrackerLensDashboard} invocation reading it back with its own,
     * independently-called {@code Instant.now()}.
     */
    private static final Duration ROBUSTNESS_DRIFT = Duration.ofHours(2);

    /**
     * How many instants across {@link #ROBUSTNESS_DRIFT} a candidate is verified at.
     *
     * <p>{@code ProgressProjector}'s lookback window is {@code now.minus(Duration.ofDays(lookbackDays))}
     * where {@code lookbackDays} is a <em>truncated</em> whole-day count - which means the single
     * record that defines "earliest" (the one {@code lookbackDays} itself is measured from) is
     * counted in the velocity window only at the razor-thin instant where elapsed time since it is
     * an exact whole number of days, and excluded at every other moment. Verifying only at
     * {@code asOf} itself would land exactly on that instant by construction and hide the swing
     * entirely - a real consuming build essentially never runs at that exact instant, so several
     * instants spread across the drift window (deliberately never {@code asOf} itself) are sampled
     * instead, to find whatever the worst swing within it actually is.
     */
    private static final int ROBUSTNESS_SAMPLE_COUNT = 8;

    /**
     * Searches for a (done-count, earliest-offset-days) pair that, when {@code buildCandidate}
     * turns it into records and the real {@link ProgressProjector} projects them, lands within
     * {@link #TOLERANCE_DAYS} of {@link FixtureSpec#forecastTargetDaysOut()} across every instant
     * sampled over {@link #ROBUSTNESS_DRIFT} (see {@link #ROBUSTNESS_SAMPLE_COUNT}), so the fixture
     * stays calibrated for however long it actually takes a consuming build to read it back, not
     * just at the exact instant it was generated.
     *
     * <p>Every candidate is verified against a real {@code ProgressProjector} instance - this
     * method never predicts what the projector will output, only tries values and reads back what
     * actually happened.</p>
     */
    private Calibration calibrate(FixtureSpec spec, int minDone, int maxDone, String finalStage, int activeCount,
            BiFunction<Integer, Integer, List<LifecycleRecord>> buildCandidate) {
        int earliestCap = Math.max(1, Math.min(90, spec.historyStartDaysAgo()));
        long target = spec.forecastTargetDaysOut();
        Instant asOf = spec.asOf();
        List<Instant> evaluationInstants = new ArrayList<>();
        for (int i = 1; i <= ROBUSTNESS_SAMPLE_COUNT; i++) {
            evaluationInstants.add(asOf.plus(ROBUSTNESS_DRIFT.multipliedBy(i).dividedBy(ROBUSTNESS_SAMPLE_COUNT)));
        }

        Calibration best = null;
        for (int doneCount = minDone; doneCount <= maxDone; doneCount++) {
            for (int earliestOffsetDays = 1; earliestOffsetDays <= earliestCap; earliestOffsetDays++) {
                List<LifecycleRecord> candidate = buildCandidate.apply(doneCount, earliestOffsetDays);
                long worstDiff = worstDiffAcrossDrift(candidate, finalStage, activeCount, evaluationInstants, asOf, target);
                if (worstDiff < 0) {
                    continue;
                }
                if (best == null || worstDiff < best.diffDays()) {
                    best = new Calibration(doneCount, earliestOffsetDays, worstDiff);
                    if (worstDiff == 0) {
                        return best;
                    }
                }
            }
        }

        if (best == null || best.diffDays() > TOLERANCE_DAYS) {
            throw new FixtureCalibrationException(
                    "trackerLensFixture: could not calibrate a forecast within " + TOLERANCE_DAYS
                    + " days of forecastTargetDaysOut=" + target + " using historyStartDaysAgo="
                    + spec.historyStartDaysAgo() + " (closest achieved: "
                    + (best == null ? "no forecast was produced at all" : best.diffDays() + " days off")
                    + "). Try a larger historyStartDaysAgo, a less aggressive forecastTargetDaysOut, or a "
                    + "larger item count to widen the range of achievable velocities.");
        }
        return best;
    }

    /**
     * The largest days-off-target {@code candidate} produces across {@code evaluationInstants},
     * measured relative to {@code asOf} (the target is always anchored to {@code asOf}, regardless
     * of which instant the projector is actually evaluated at) - or {@code -1} if any instant
     * produces no projection at all.
     */
    private long worstDiffAcrossDrift(List<LifecycleRecord> candidate, String finalStage, int activeCount,
            List<Instant> evaluationInstants, Instant asOf, long target) {
        long worstDiff = -1;
        for (Instant evalNow : evaluationInstants) {
            Optional<Projection> projection = progressProjector.project(candidate, finalStage, activeCount, evalNow);
            if (projection.isEmpty()) {
                return -1;
            }
            long actualDaysOut = Duration.between(asOf, projection.get().projectedDate()).toDays();
            worstDiff = Math.max(worstDiff, Math.abs(actualDaysOut - target));
        }
        return worstDiff;
    }

    private record Calibration(int doneCount, int earliestOffsetDays, long diffDays) {
    }
}
