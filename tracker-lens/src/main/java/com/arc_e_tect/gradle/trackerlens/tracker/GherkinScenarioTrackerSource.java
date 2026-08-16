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
 * {@link TrackerSource} for {@code gherkin-to-asciidoc}'s scenario progress-history NDJSON, one
 * record per line with fields {@code fingerprint}, {@code scenarioName}, {@code featureTitle},
 * {@code listedAt}, {@code definedAt}, {@code implementedAt}, {@code lastSeenAt}, {@code removedAt}.
 *
 * <p>This module has no code dependency on {@code gherkin-to-asciidoc}; the schema below is
 * hand-parsed directly from the known, fixed NDJSON shape that plugin writes, the same way that
 * plugin's own store hand-parses it - see this class's Javadoc rather than any shared code for the
 * field-by-field mapping.</p>
 *
 * <p>Stages, in canonical order: {@code listed}, {@code defined}, {@code implemented}.</p>
 */
public class GherkinScenarioTrackerSource implements TrackerSource {

    private static final Logger LOGGER = Logging.getLogger(GherkinScenarioTrackerSource.class);

    /**
     * Matches the file-level {@code {"schemaVersion":N}} marker line {@code gherkin-to-asciidoc}'s
     * {@code ProgressHistoryStore} writes as of schema version 1 - present as this class's first
     * line, silently skipped rather than logged as malformed. A file written before the marker
     * existed simply has no line matching this pattern, so it reads exactly as it always has.
     */
    private static final Pattern SCHEMA_VERSION_LINE = Pattern.compile("^\\{\"schemaVersion\":(\\d+)\\}$");

    private static final String STRING_FIELD = "\"((?:[^\"\\\\]|\\\\.)*)\"";
    private static final String INSTANT_FIELD = "(null|\"[^\"]*\")";
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "^\\{"
            + "\"fingerprint\":" + STRING_FIELD + ","
            + "\"scenarioName\":" + STRING_FIELD + ","
            + "\"featureTitle\":" + STRING_FIELD + ","
            + "\"listedAt\":" + INSTANT_FIELD + ","
            + "\"definedAt\":" + INSTANT_FIELD + ","
            + "\"implementedAt\":" + INSTANT_FIELD + ","
            + "\"lastSeenAt\":" + INSTANT_FIELD + ","
            + "\"removedAt\":" + INSTANT_FIELD
            + "\\}$");

    /** Creates a new {@code GherkinScenarioTrackerSource}. */
    public GherkinScenarioTrackerSource() {}

    @Override
    public List<LifecycleRecord> read(File historyFile) {
        List<String> lines;
        try {
            lines = Files.readAllLines(historyFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GradleException("trackerLens: could not read gherkin scenario history file: " + historyFile, e);
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
            String scenarioName = unescape(matcher.group(2));
            String featureTitle = unescape(matcher.group(3));
            Instant listedAt = parseInstant(matcher.group(4));
            Instant definedAt = parseInstant(matcher.group(5));
            Instant implementedAt = parseInstant(matcher.group(6));
            Instant lastSeenAt = parseInstant(matcher.group(7));
            Instant removedAt = parseInstant(matcher.group(8));

            Map<String, Instant> stages = new LinkedHashMap<>();
            if (listedAt != null) {
                stages.put("listed", listedAt);
            }
            if (definedAt != null) {
                stages.put("defined", definedAt);
            }
            if (implementedAt != null) {
                stages.put("implemented", implementedAt);
            }

            return new LifecycleRecord(fingerprint, scenarioName, featureTitle, stages, lastSeenAt, removedAt);
        } catch (DateTimeParseException e) {
            return null;
        }
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
