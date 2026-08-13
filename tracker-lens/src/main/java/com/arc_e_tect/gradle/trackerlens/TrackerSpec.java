package com.arc_e_tect.gradle.trackerlens;

import java.io.File;
import java.util.List;

/**
 * A resolved {@code trackerLens.trackers { ... }} registration, as read by
 * {@link GenerateTrackerLensTask} from {@link TrackerLensExtension#getTrackers()} at configuration
 * time.
 *
 * @param id           the tracker's registered name
 * @param kind         which built-in tracker source reads each of {@code historyFiles}
 * @param historyFiles the configured history file/directory entries; a directory is expanded to
 *                      its {@code *.ndjson} children at task-run time, and any entry may not exist
 */
public record TrackerSpec(String id, TrackerSourceKind kind, List<File> historyFiles) {
}
