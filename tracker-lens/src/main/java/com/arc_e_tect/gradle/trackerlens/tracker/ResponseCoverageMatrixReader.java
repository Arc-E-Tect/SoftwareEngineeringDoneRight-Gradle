package com.arc_e_tect.gradle.trackerlens.tracker;

import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the same NDJSON file {@link ResponseCoverageTrackerSource} reads, but into
 * {@link ResponseCoverageCell}s - the current-state snapshot pivot a coverage-depth grid needs,
 * which {@link LifecycleRecord}'s milestone-timestamp shape cannot carry (no field for
 * {@code testCount}, and {@code verb}/{@code path}/{@code responseCode} would need to stay
 * independently pivotable rather than fused into one {@code label} string).
 *
 * <p>Only currently-declared rows are returned (a non-null {@code removedAt} is skipped) -
 * consistent with this plugin's own "exclude removed items from current-state views" convention
 * for {@code chartSeries}/projection counts elsewhere. There is deliberately no notion of "as of a
 * past date" here: {@code testCount} is a live gauge the source NDJSON never preserves a history
 * of, so a matrix can only ever reflect the most recent scan.</p>
 */
public class ResponseCoverageMatrixReader {

    private static final Logger LOGGER = Logging.getLogger(ResponseCoverageMatrixReader.class);

    private static final Pattern SCHEMA_VERSION_LINE = Pattern.compile("^\\{\"schemaVersion\":(\\d+)\\}$");

    private static final String STRING_FIELD = "\"((?:[^\"\\\\]|\\\\.)*)\"";
    // Non-capturing: firstDeclaredAt/firstCoveredAt/lastSeenAt are read by neither this class nor
    // ResponseCoverageCell, unlike removedAt below - keeping them out of the group numbering below.
    private static final String INSTANT_FIELD = "(?:null|\"[^\"]*\")";
    // Capturing group numbers, given the above: 1=fingerprint, 2=verb, 3=path, 4=responseCode,
    // 5=testCount, 6=removedAt.
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "^\\{"
            + "\"fingerprint\":" + STRING_FIELD + ","
            + "\"verb\":" + STRING_FIELD + ","
            + "\"path\":" + STRING_FIELD + ","
            + "\"responseCode\":" + STRING_FIELD + ","
            + "\"testCount\":(\\d+),"
            + "\"firstDeclaredAt\":" + INSTANT_FIELD + ","
            + "\"firstCoveredAt\":" + INSTANT_FIELD + ","
            + "\"lastSeenAt\":" + INSTANT_FIELD + ","
            + "\"removedAt\":(null|\"[^\"]*\")"
            + "\\}$");

    /** Creates a new {@code ResponseCoverageMatrixReader}. */
    public ResponseCoverageMatrixReader() {}

    /**
     * Reads {@code historyFile} and returns every currently-declared cell.
     *
     * @param historyFile the NDJSON history file; guaranteed by the caller to exist
     * @return the currently-declared cells; a line that fails to parse, or whose {@code removedAt}
     *         is non-null, is skipped
     */
    public List<ResponseCoverageCell> read(File historyFile) {
        List<String> lines;
        try {
            lines = Files.readAllLines(historyFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GradleException(
                    "trackerLens: could not read response coverage history file: " + historyFile, e);
        }

        List<ResponseCoverageCell> cells = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            if (i == 0 && SCHEMA_VERSION_LINE.matcher(line).matches()) {
                continue;
            }
            Matcher matcher = LINE_PATTERN.matcher(line);
            if (!matcher.matches()) {
                LOGGER.warn("trackerLens: skipping malformed line {} in {}", i + 1, historyFile);
                continue;
            }
            if (!"null".equals(matcher.group(6))) {
                continue;
            }
            String path = unescape(matcher.group(3));
            String responseCode = unescape(matcher.group(4));
            int testCount = Integer.parseInt(matcher.group(5));
            cells.add(new ResponseCoverageCell(matcher.group(2), path, responseCode, testCount, testCount > 0));
        }
        return cells;
    }

    private String unescape(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                i++;
                result.append(value.charAt(i));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
