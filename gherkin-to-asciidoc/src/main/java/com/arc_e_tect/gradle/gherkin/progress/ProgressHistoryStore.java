package com.arc_e_tect.gradle.gherkin.progress;

import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes {@link ScenarioProgressRecord}s as newline-delimited JSON (NDJSON), one record
 * per line, sorted by {@link ScenarioProgressRecord#fingerprint()} so that git diffs of the
 * persisted file are minimal and stable regardless of scenario reordering or scenarios moving
 * between feature files.
 *
 * <p>The schema is fixed and flat, so the serializer/parser is hand-rolled (no JSON library
 * dependency) - only {@link ScenarioProgressRecord#scenarioName()} and
 * {@link ScenarioProgressRecord#featureTitle()} are free text and need escaping.</p>
 */
public class ProgressHistoryStore {

    private static final Logger LOGGER = Logging.getLogger(ProgressHistoryStore.class);

    /**
     * The current schema version, written as the file's own first line ({@code save} always emits
     * it) so a future format change has something to branch reading logic on. Purely additive: a
     * file written before this existed has no such line, and {@link #load(File)} tolerates that by
     * treating a missing marker as version 1, exactly like a present {@code "schemaVersion":1} line.
     */
    private static final int CURRENT_SCHEMA_VERSION = 1;
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

    /** Creates a new {@code ProgressHistoryStore}. */
    public ProgressHistoryStore() {}

    /**
     * Loads the progress history from {@code file}.
     *
     * @param file the NDJSON history file; need not exist
     * @return the records keyed by fingerprint; empty when {@code file} doesn't exist. A line that
     *         fails to parse is skipped with a {@code WARN}-level log message identifying the line
     *         number - it never fails the build.
     */
    public Map<String, ScenarioProgressRecord> load(File file) {
        Map<String, ScenarioProgressRecord> records = new LinkedHashMap<>();
        if (!file.isFile()) {
            return records;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GradleException("gherkinToAsciidoc: could not read progress history file: " + file, e);
        }

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            if (i == 0 && SCHEMA_VERSION_LINE.matcher(line).matches()) {
                continue;
            }
            ScenarioProgressRecord record = parseLine(line);
            if (record == null) {
                LOGGER.warn("gherkinToAsciidoc: skipping malformed progress history line {} in {}", i + 1, file);
                continue;
            }
            records.put(record.fingerprint(), record);
        }
        return records;
    }

    /**
     * Writes {@code records} to {@code file} as NDJSON, sorted by
     * {@link ScenarioProgressRecord#fingerprint()}, overwriting any existing content.
     *
     * @param file    the NDJSON history file to write
     * @param records the records to persist
     */
    public void save(File file, Collection<ScenarioProgressRecord> records) {
        List<ScenarioProgressRecord> sorted = new ArrayList<>(records);
        sorted.sort(Comparator.comparing(ScenarioProgressRecord::fingerprint));

        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.println("{\"schemaVersion\":" + CURRENT_SCHEMA_VERSION + "}");
            for (ScenarioProgressRecord record : sorted) {
                writer.println(toJson(record));
            }
        } catch (IOException e) {
            throw new GradleException("gherkinToAsciidoc: could not write progress history file: " + file, e);
        }
    }

    private String toJson(ScenarioProgressRecord record) {
        return "{"
                + "\"fingerprint\":\"" + record.fingerprint() + "\","
                + "\"scenarioName\":\"" + escape(record.scenarioName()) + "\","
                + "\"featureTitle\":\"" + escape(record.featureTitle()) + "\","
                + "\"listedAt\":" + instantJson(record.listedAt()) + ","
                + "\"definedAt\":" + instantJson(record.definedAt()) + ","
                + "\"implementedAt\":" + instantJson(record.implementedAt()) + ","
                + "\"lastSeenAt\":" + instantJson(record.lastSeenAt()) + ","
                + "\"removedAt\":" + instantJson(record.removedAt())
                + "}";
    }

    private String instantJson(Instant instant) {
        return instant == null ? "null" : "\"" + instant + "\"";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
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

    private ScenarioProgressRecord parseLine(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return new ScenarioProgressRecord(
                    matcher.group(1),
                    unescape(matcher.group(2)),
                    unescape(matcher.group(3)),
                    parseInstant(matcher.group(4)),
                    parseInstant(matcher.group(5)),
                    parseInstant(matcher.group(6)),
                    parseInstant(matcher.group(7)),
                    parseInstant(matcher.group(8)));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private Instant parseInstant(String jsonValue) {
        return "null".equals(jsonValue) ? null : Instant.parse(jsonValue.substring(1, jsonValue.length() - 1));
    }
}
