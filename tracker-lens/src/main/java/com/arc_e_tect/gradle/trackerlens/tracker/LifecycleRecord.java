package com.arc_e_tect.gradle.trackerlens.tracker;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A single tracked item's lifecycle, as read from a {@link TrackerSource}.
 *
 * <p>{@code stageReachedAt} is ordered by the tracker's canonical stage sequence (e.g.
 * {@code listed}, {@code defined}, {@code implemented}) and only contains entries for stages the
 * item has actually reached; a stage never (yet) reached is simply absent from the map rather than
 * mapped to {@code null}.</p>
 *
 * @param id            stable identifier for this item, unique within its tracker
 * @param label         human-readable display name
 * @param group         optional grouping label (e.g. the enclosing feature or declaring class),
 *                       or {@code null} when the source has no grouping concept
 * @param stageReachedAt ordered map of stage name to the instant the item first reached that stage
 * @param lastSeenAt    when the item was last observed present, or {@code null} if never observed
 * @param removedAt     when the item was first observed missing, or {@code null} while still present
 */
public record LifecycleRecord(
        String id,
        String label,
        String group,
        Map<String, Instant> stageReachedAt,
        Instant lastSeenAt,
        Instant removedAt) {

    /** Defensively copies {@code stageReachedAt} into an unmodifiable, order-preserving map. */
    public LifecycleRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(stageReachedAt, "stageReachedAt");
        // Map.copyOf does not preserve iteration order; this map's insertion order is the contract.
        stageReachedAt = Collections.unmodifiableMap(new LinkedHashMap<>(stageReachedAt));
    }

    /**
     * Whether this item has reached {@code stage}.
     *
     * @param stage the stage name to check
     * @return {@code true} if {@code stageReachedAt} contains {@code stage}
     */
    public boolean hasReached(String stage) {
        return stageReachedAt.containsKey(stage);
    }

    /**
     * The furthest stage this item has reached, per {@code canonicalStages}' own order.
     *
     * <p>Not simply "the highest-index key present in {@link #stageReachedAt()}", since some
     * {@link TrackerSource}s stamp only the single stage matching an item's current status rather
     * than backfilling every earlier stage it must logically have passed through - the Gherkin
     * scenario source, for one, never gains a {@code defined} entry for a scenario first observed
     * already {@code implemented}. This walks {@code canonicalStages} from the end instead, so it
     * always reports the furthest reached stage regardless of which earlier ones were skipped.</p>
     *
     * @param canonicalStages the tracker's canonical stage names, in order
     * @return the furthest reached stage, or empty if none of {@code canonicalStages} has been
     *         reached
     */
    public Optional<String> latestStage(List<String> canonicalStages) {
        for (int i = canonicalStages.size() - 1; i >= 0; i--) {
            String stage = canonicalStages.get(i);
            if (stageReachedAt.containsKey(stage)) {
                return Optional.of(stage);
            }
        }
        return Optional.empty();
    }
}
