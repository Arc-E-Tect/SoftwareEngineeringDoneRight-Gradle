package com.arc_e_tect.gradle.trackerlens.tracker;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

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
}
