package com.arc_e_tect.gradle.trackerlens.tracker;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Merges {@link LifecycleRecord}s sharing the same {@link LifecycleRecord#id()} - read from
 * different history files registered on the same tracker - into one record per id.
 *
 * <p>This is what makes splitting one tracker's history across several files (or a directory of
 * them, see {@link HistoryFileResolver}) safe: without it, an item observed in more than one file
 * (e.g. an endpoint that both {@code shadow-api-detector}'s and {@code mirage-api-detector}'s own
 * history files know about, when a user points each detector at its own file instead of a shared
 * one) would render as duplicate rows with only a partial view of its lifecycle in each.</p>
 *
 * <p>Always applied, even when a tracker resolves to exactly one file: merging a set of records
 * with no id collisions is a no-op, so there is no separate single-file code path to keep in
 * sync.</p>
 */
public class LifecycleRecordMerger {

    /** Creates a new {@code LifecycleRecordMerger}. */
    public LifecycleRecordMerger() {}

    /**
     * Merges {@code records} by id.
     *
     * @param canonicalStages the tracker's canonical stage names, in order - determines both the
     *                        merged record's {@code stageReachedAt} ordering and, for a stage
     *                        reported by more than one record for the same id, which timestamp
     *                        wins (the earliest)
     * @param records         the records to merge, from every resolved history file
     * @return one record per distinct id, in first-seen order
     */
    public List<LifecycleRecord> merge(List<String> canonicalStages, List<LifecycleRecord> records) {
        Map<String, List<LifecycleRecord>> byId = new LinkedHashMap<>();
        for (LifecycleRecord record : records) {
            byId.computeIfAbsent(record.id(), id -> new ArrayList<>()).add(record);
        }

        List<LifecycleRecord> merged = new ArrayList<>();
        for (List<LifecycleRecord> group : byId.values()) {
            merged.add(mergeGroup(canonicalStages, group));
        }
        return merged;
    }

    private LifecycleRecord mergeGroup(List<String> canonicalStages, List<LifecycleRecord> group) {
        if (group.size() == 1) {
            return group.get(0);
        }

        String label = group.stream()
                .map(LifecycleRecord::label)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElseGet(() -> group.get(0).label());

        String groupLabel = group.stream()
                .map(LifecycleRecord::group)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        Map<String, Instant> stageReachedAt = new LinkedHashMap<>();
        for (String stage : canonicalStages) {
            group.stream()
                    .map(record -> record.stageReachedAt().get(stage))
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .ifPresent(earliest -> stageReachedAt.put(stage, earliest));
        }

        Instant lastSeenAt = group.stream()
                .map(LifecycleRecord::lastSeenAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        // Only merged-removed when every source agrees the item is gone - a source that has
        // simply stopped writing this file must never make an item still tracked elsewhere
        // appear removed.
        boolean removedEverywhere = group.stream().allMatch(record -> record.removedAt() != null);
        Instant removedAt = removedEverywhere
                ? group.stream().map(LifecycleRecord::removedAt).max(Comparator.naturalOrder()).orElse(null)
                : null;

        return new LifecycleRecord(group.get(0).id(), label, groupLabel, stageReachedAt, lastSeenAt, removedAt);
    }
}
