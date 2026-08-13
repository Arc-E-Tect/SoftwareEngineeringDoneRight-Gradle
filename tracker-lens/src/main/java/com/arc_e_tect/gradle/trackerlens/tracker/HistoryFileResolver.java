package com.arc_e_tect.gradle.trackerlens.tracker;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Expands a tracker's configured {@code historyFiles} entries into a flat, deterministic list of
 * concrete NDJSON files to read: an entry that is itself a file is used as-is, and an entry that is
 * a directory is expanded to every {@code *.ndjson} file directly within it (non-recursive), sorted
 * by name.
 *
 * <p>This is what lets {@code trackerLens.trackers.register("...") { historyFiles.from(...) }}
 * accept either individual files or a directory of them - useful when a tracker's history is split
 * across several files, e.g. one contract history file per API-detector plugin.</p>
 */
public class HistoryFileResolver {

    private static final String HISTORY_FILE_SUFFIX = ".ndjson";

    /** Creates a new {@code HistoryFileResolver}. */
    public HistoryFileResolver() {}

    /**
     * Expands {@code configuredEntries} into concrete files.
     *
     * @param configuredEntries the tracker's configured {@code historyFiles} entries; an entry
     *                          need not exist
     * @return the concrete files to read, directories expanded in place; an entry that doesn't
     *         exist is passed through unchanged (the caller is responsible for skipping it)
     */
    public List<File> resolve(Collection<File> configuredEntries) {
        List<File> resolved = new ArrayList<>();
        for (File entry : configuredEntries) {
            if (entry.isDirectory()) {
                resolved.addAll(listHistoryFiles(entry));
            } else {
                resolved.add(entry);
            }
        }
        return resolved;
    }

    private List<File> listHistoryFiles(File directory) {
        File[] children = directory.listFiles((dir, name) -> name.endsWith(HISTORY_FILE_SUFFIX));
        if (children == null) {
            return List.of();
        }
        List<File> sorted = new ArrayList<>(Arrays.asList(children));
        sorted.sort(Comparator.comparing(File::getName));
        return sorted;
    }
}
