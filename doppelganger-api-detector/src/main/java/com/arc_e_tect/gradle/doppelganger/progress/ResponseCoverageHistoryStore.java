package com.arc_e_tect.gradle.doppelganger.progress;

import com.arc_e_tect.gradle.detector.core.model.HttpVerb;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes {@link ResponseCoverageRecord}s as newline-delimited JSON (NDJSON), one record
 * per line, sorted by {@link ResponseCoverageRecord#fingerprint()} so that git diffs of the
 * persisted file are minimal and stable - the same hand-rolled, dependency-free approach
 * {@code api-detector-core}'s {@code ContractHistoryStore} uses for the cross-plugin contract
 * progress history.
 */
public class ResponseCoverageHistoryStore {

    private static final Logger LOGGER = Logger.getLogger(ResponseCoverageHistoryStore.class.getName());

    private static final int CURRENT_SCHEMA_VERSION = 1;
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

    /** Creates a new {@code ResponseCoverageHistoryStore}. */
    public ResponseCoverageHistoryStore() {}

    /**
     * Loads the response coverage history from {@code file}.
     *
     * @param file the NDJSON history file; need not exist
     * @return the records keyed by fingerprint; empty when {@code file} doesn't exist. A line that
     *         fails to parse is skipped with a {@code WARN}-level log message identifying the line
     *         number - it never fails the build.
     */
    public Map<String, ResponseCoverageRecord> load(File file) {
        Map<String, ResponseCoverageRecord> records = new LinkedHashMap<>();
        if (!file.isFile()) {
            return records;
        }

        List<String> lines = readLines(file);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            if (i == 0 && SCHEMA_VERSION_LINE.matcher(line).matches()) {
                continue;
            }
            ResponseCoverageRecord record = parseLine(line);
            if (record != null) {
                records.put(record.fingerprint(), record);
                continue;
            }
            LOGGER.log(Level.WARNING,
                    "doppelgangerApiDetector: skipping malformed response coverage history line {0} in {1}",
                    new Object[] {i + 1, file});
        }
        return records;
    }

    /**
     * Writes {@code records} to {@code file} as NDJSON, sorted by
     * {@link ResponseCoverageRecord#fingerprint()}, overwriting any existing content.
     *
     * @param file    the NDJSON history file to write
     * @param records the records to persist
     */
    public void save(File file, Collection<ResponseCoverageRecord> records) {
        List<ResponseCoverageRecord> sorted = new ArrayList<>(records);
        sorted.sort(Comparator.comparing(ResponseCoverageRecord::fingerprint));

        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.println("{\"schemaVersion\":" + CURRENT_SCHEMA_VERSION + "}");
            for (ResponseCoverageRecord record : sorted) {
                writer.println(toJson(record));
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "doppelgangerApiDetector: could not write response coverage history file: " + file, e);
        }
    }

    private List<String> readLines(File file) {
        try {
            return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "doppelgangerApiDetector: could not read response coverage history file: " + file, e);
        }
    }

    private String toJson(ResponseCoverageRecord record) {
        return "{"
                + "\"fingerprint\":\"" + record.fingerprint() + "\","
                + "\"verb\":\"" + record.verb().name() + "\","
                + "\"path\":\"" + escape(record.path()) + "\","
                + "\"responseCode\":\"" + escape(record.responseCode()) + "\","
                + "\"testCount\":" + record.testCount() + ","
                + "\"firstDeclaredAt\":" + instantJson(record.firstDeclaredAt()) + ","
                + "\"firstCoveredAt\":" + instantJson(record.firstCoveredAt()) + ","
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

    private ResponseCoverageRecord parseLine(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return new ResponseCoverageRecord(
                    matcher.group(1),
                    HttpVerb.valueOf(matcher.group(2)),
                    unescape(matcher.group(3)),
                    unescape(matcher.group(4)),
                    Integer.parseInt(matcher.group(5)),
                    parseInstant(matcher.group(6)),
                    parseInstant(matcher.group(7)),
                    parseInstant(matcher.group(8)),
                    parseInstant(matcher.group(9)));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            return null;
        }
    }

    private Instant parseInstant(String jsonValue) {
        return "null".equals(jsonValue) ? null : Instant.parse(jsonValue.substring(1, jsonValue.length() - 1));
    }
}
