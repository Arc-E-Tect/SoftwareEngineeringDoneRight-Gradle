package com.arc_e_tect.gradle.trackerlens.tracker;

import java.io.File;
import java.util.List;

/**
 * Reads a producing plugin's NDJSON lifecycle-history file into {@link LifecycleRecord}s.
 *
 * <p>Each implementation hand-parses the known, fixed NDJSON shape of one specific producing
 * plugin directly - this module has no code dependency on any producing plugin, so nothing here
 * shares a class with, say, {@code gherkin-to-asciidoc}'s own history record. A third
 * implementation can be added later (for a new kind of tracked history) purely by implementing
 * this interface and, if it should be selectable from the {@code trackerLens} DSL, adding a
 * corresponding constant to {@code TrackerSourceKind} - nothing else in this module needs to
 * change, since {@link com.arc_e_tect.gradle.trackerlens.GenerateTrackerLensTask} and the
 * dashboard renderer only ever operate on this interface and on {@link LifecycleRecord}.</p>
 */
public interface TrackerSource {

    /**
     * Reads {@code historyFile} and returns the {@link LifecycleRecord}s it contains.
     *
     * @param historyFile the NDJSON history file; guaranteed by the caller to exist
     * @return the parsed records; a line that fails to parse is skipped rather than failing the read
     */
    List<LifecycleRecord> read(File historyFile);
}
