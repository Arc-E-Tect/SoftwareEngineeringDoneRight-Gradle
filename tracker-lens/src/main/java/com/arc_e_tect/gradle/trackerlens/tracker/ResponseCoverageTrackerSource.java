package com.arc_e_tect.gradle.trackerlens.tracker;

import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link TrackerSource} for the Doppelganger API Detector's response-coverage-history NDJSON, one
 * record per line with fields {@code fingerprint}, {@code verb}, {@code path},
 * {@code responseCode}, {@code testCount}, {@code firstDeclaredAt}, {@code firstCoveredAt},
 * {@code lastSeenAt}, {@code removedAt}.
 *
 * <p>This module has no code dependency on {@code doppelganger-api-detector}; the schema below is
 * hand-parsed directly from the known, fixed NDJSON shape that plugin's own
 * {@code ResponseCoverageHistoryStore} writes - see this class's Javadoc rather than any shared
 * code for the field-by-field mapping.</p>
 *
 * <p>Stages, in canonical order: {@code declared}, {@code covered} - a true dependency chain, since
 * a response code can never be covered by a contract test without first being declared. Each
 * record's {@link LifecycleRecord#group()} is the response code's class ({@code "2xx"},
 * {@code "4xx"}, {@code "5xx"}, ...), derived from {@code responseCode} alone - see
 * {@link #responseCodeClass(String)} - so downstream group-partitioned views (e.g. a
 * coverage-by-class chart) need no further code dependency on this class either, only on
 * {@link LifecycleRecord#group()} itself. {@code testCount} - a live gauge, not a milestone - is
 * deliberately not carried onto {@link LifecycleRecord} at all, since that type has no field for
 * one; a template needing it reads {@link ResponseCoverageMatrixReader}'s output instead.</p>
 */
public class ResponseCoverageTrackerSource implements TrackerSource {

    private static final Logger LOGGER = Logging.getLogger(ResponseCoverageTrackerSource.class);

    /**
     * Matches an optional file-level {@code {"schemaVersion":N}} marker line, silently skipped
     * rather than logged as malformed if present as this file's first line.
     */
    private static final Pattern SCHEMA_VERSION_LINE = Pattern.compile("^\\{\"schemaVersion\":(\\d+)\\}$");

    private static final String STRING_FIELD = "\"((?:[^\"\\\\]|\\\\.)*)\"";
    private static final String INSTANT_FIELD = "(null|\"[^\"]*\")";
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
            + "\"removedAt\":" + INSTANT_FIELD
            + "\\}$");

    /** Creates a new {@code ResponseCoverageTrackerSource}. */
    public ResponseCoverageTrackerSource() {}

    @Override
    public List<LifecycleRecord> read(File historyFile) {
        List<String> lines;
        try {
            lines = Files.readAllLines(historyFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GradleException(
                    "trackerLens: could not read response coverage history file: " + historyFile, e);
        }

        List<LifecycleRecord> records = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            if (i == 0 && SCHEMA_VERSION_LINE.matcher(line).matches()) {
                continue;
            }
            LifecycleRecord record = parseLine(line);
            if (record == null) {
                LOGGER.warn("trackerLens: skipping malformed line {} in {}", i + 1, historyFile);
                continue;
            }
            records.add(record);
        }
        return records;
    }

    private LifecycleRecord parseLine(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return null;
        }
        try {
            String fingerprint = matcher.group(1);
            String verb = matcher.group(2);
            String path = unescape(matcher.group(3));
            String responseCode = unescape(matcher.group(4));
            Instant firstDeclaredAt = parseInstant(matcher.group(6));
            Instant firstCoveredAt = parseInstant(matcher.group(7));
            Instant lastSeenAt = parseInstant(matcher.group(8));
            Instant removedAt = parseInstant(matcher.group(9));

            Map<String, Instant> stages = new LinkedHashMap<>();
            if (firstDeclaredAt != null) {
                stages.put("declared", firstDeclaredAt);
            }
            if (firstCoveredAt != null) {
                stages.put("covered", firstCoveredAt);
            }

            String label = verb + " " + path + " " + responseCode;
            return new LifecycleRecord(
                    fingerprint, label, responseCodeClass(responseCode), stages, lastSeenAt, removedAt);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * The response code's class - {@code "2xx"}, {@code "4xx"}, {@code "5xx"}, etc. - derived from
     * its first character alone, so a literal range wildcard (e.g. {@code "5XX"}) already lands in
     * its own class without any special-casing. Anything that doesn't start with a digit 1-5 (e.g.
     * {@code "default"}) falls into {@code "other"}.
     *
     * @param responseCode the raw response code, e.g. {@code "200"}, {@code "5XX"}, {@code "default"}
     * @return the response code's class
     */
    static String responseCodeClass(String responseCode) {
        if (responseCode.isEmpty()) {
            return "other";
        }
        char first = responseCode.charAt(0);
        return first >= '1' && first <= '5' ? first + "xx" : "other";
    }

    private Instant parseInstant(String jsonValue) {
        return "null".equals(jsonValue) ? null : Instant.parse(jsonValue.substring(1, jsonValue.length() - 1));
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
