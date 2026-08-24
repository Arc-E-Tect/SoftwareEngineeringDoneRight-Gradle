package com.arc_e_tect.gradle.doppelganger.report;

import com.arc_e_tect.gradle.doppelganger.progress.ResponseCoverageRecord;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Writes the {@code == Response Coverage Over Time} AsciiDoc table section for the
 * {@code scanContracts} report, from a loaded/advanced {@link ResponseCoverageRecord} history map.
 */
public class ResponseCoverageTableWriter {

    private static final DateTimeFormatter TRACKED_SINCE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    /** Creates a new {@code ResponseCoverageTableWriter}. */
    public ResponseCoverageTableWriter() {}

    /**
     * Writes the {@code == Response Coverage Over Time} section to {@code writer}, or nothing at
     * all when {@code history} is empty.
     *
     * @param writer  the AsciiDoc output to append to
     * @param history the response coverage history to summarise, keyed by fingerprint
     */
    public void write(PrintWriter writer, Map<String, ResponseCoverageRecord> history) {
        if (history.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        Instant trackedSince = history.values().stream()
                .flatMap(record -> Stream.of(
                        record.firstDeclaredAt(), record.firstCoveredAt(), record.lastSeenAt(), record.removedAt()))
                .filter(instant -> instant != null)
                .min(Comparator.naturalOrder())
                .orElse(null);

        long trackedCodes = history.values().stream().filter(record -> record.removedAt() == null).count();
        long coveredCodes = history.values().stream()
                .filter(record -> record.removedAt() == null && record.testCount() > 0)
                .count();
        long removedNotSeen = history.values().stream().filter(record -> record.removedAt() != null).count();

        writer.println("== Response Coverage Over Time");
        writer.println();
        writer.println("[cols=\"1,1\",options=\"header\"]");
        writer.println("|===");
        writer.println("| Metric | Value");
        writer.println();
        writer.println("| Tracked since");
        writer.println("| " + (trackedSince != null ? TRACKED_SINCE_FORMATTER.format(trackedSince) : "N/A"));
        writer.println();
        writer.println("| Response codes currently tracked");
        writer.println("| " + trackedCodes);
        writer.println();
        writer.println("| Response codes currently covered by at least one test");
        writer.println("| " + coveredCodes);
        writer.println();
        writeWindowedMetric(writer, "Newly covered", history, now);
        writer.println("| Removed (no longer declared)");
        writer.println("| " + removedNotSeen);
        writer.println("|===");
        writer.println();
    }

    private void writeWindowedMetric(
            PrintWriter writer, String label, Map<String, ResponseCoverageRecord> history, Instant now) {
        writer.println("| " + label + " in the last 7 days");
        writer.println("| " + countWithin(history, now, Duration.ofDays(7)));
        writer.println();
        writer.println("| " + label + " in the last 30 days");
        writer.println("| " + countWithin(history, now, Duration.ofDays(30)));
        writer.println();
    }

    private long countWithin(Map<String, ResponseCoverageRecord> history, Instant now, Duration window) {
        Instant threshold = now.minus(window);
        return history.values().stream()
                .map(ResponseCoverageRecord::firstCoveredAt)
                .filter(coveredAt -> coveredAt != null && coveredAt.isAfter(threshold))
                .count();
    }
}
