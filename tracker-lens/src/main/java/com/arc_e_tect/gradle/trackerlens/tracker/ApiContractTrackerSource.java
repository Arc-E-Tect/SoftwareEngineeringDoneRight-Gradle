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
 * {@link TrackerSource} for the API-detector plugins' shared contract progress-history NDJSON,
 * one record per line with fields {@code fingerprint}, {@code verb}, {@code path},
 * {@code declaringClass}, {@code declaredAt}, {@code implementedAt}, {@code stubbedAt},
 * {@code verifiedAt}, {@code lastSeenAt}, {@code removedAt}.
 *
 * <p>This module has no code dependency on {@code api-detector-core}; the schema below is
 * hand-parsed directly from the known, fixed NDJSON shape that library writes, the same way that
 * library's own store hand-parses it - see this class's Javadoc rather than any shared code for
 * the field-by-field mapping. This is the <em>current</em>, 10-field shape; a 9-field file written
 * before {@code stubbedAt} existed does not match {@link #LINE_PATTERN} at all and every one of
 * its lines is logged and skipped as malformed - see {@code api-detector-core}'s
 * {@code LegacyContractHistoryFormatException} for the equivalent, more specific failure on the
 * writing side.</p>
 *
 * <p>Stages, in canonical order: {@code declared}, {@code implemented}, {@code stubbed},
 * {@code verified}.</p>
 */
public class ApiContractTrackerSource implements TrackerSource {

    private static final Logger LOGGER = Logging.getLogger(ApiContractTrackerSource.class);

    /**
     * Matches the legacy, bare-integer {@code {"schemaVersion":N}} marker line
     * {@code api-detector-core} wrote before it tracked a semver format version, silently skipped
     * rather than logged as malformed if present as this file's first line.
     */
    private static final Pattern LEGACY_SCHEMA_VERSION_LINE = Pattern.compile("^\\{\"schemaVersion\":(\\d+)\\}$");

    /**
     * Matches the current {@code {"schemaVersion":"x.y.z"[,"migrations":[...]]}} marker line
     * {@code api-detector-core}'s {@code ContractHistoryStore} writes - a semver format version,
     * optionally followed by its own persisted migration audit trail. Skipped the same way
     * {@link #LEGACY_SCHEMA_VERSION_LINE} is; this reader has no need for either the version or
     * the audit trail itself, only for not misreading the line that carries them as a malformed
     * record.
     */
    private static final Pattern SCHEMA_VERSION_LINE = Pattern.compile(
            "^\\{\"schemaVersion\":\"[^\"]+\"(?:,\"migrations\":\\[.*])?}$");

    private static final String STRING_FIELD = "\"((?:[^\"\\\\]|\\\\.)*)\"";
    private static final String NULLABLE_STRING_FIELD = "(?:null|" + STRING_FIELD + ")";
    private static final String INSTANT_FIELD = "(null|\"[^\"]*\")";
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "^\\{"
            + "\"fingerprint\":" + STRING_FIELD + ","
            + "\"verb\":" + STRING_FIELD + ","
            + "\"path\":" + STRING_FIELD + ","
            + "\"declaringClass\":" + NULLABLE_STRING_FIELD + ","
            + "\"declaredAt\":" + INSTANT_FIELD + ","
            + "\"implementedAt\":" + INSTANT_FIELD + ","
            + "\"stubbedAt\":" + INSTANT_FIELD + ","
            + "\"verifiedAt\":" + INSTANT_FIELD + ","
            + "\"lastSeenAt\":" + INSTANT_FIELD + ","
            + "\"removedAt\":" + INSTANT_FIELD
            + "\\}$");

    /** Creates a new {@code ApiContractTrackerSource}. */
    public ApiContractTrackerSource() {}

    @Override
    public List<LifecycleRecord> read(File historyFile) {
        List<String> lines;
        try {
            lines = Files.readAllLines(historyFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GradleException("trackerLens: could not read API contract history file: " + historyFile, e);
        }

        List<LifecycleRecord> records = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            if (i == 0 && (LEGACY_SCHEMA_VERSION_LINE.matcher(line).matches()
                    || SCHEMA_VERSION_LINE.matcher(line).matches())) {
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
            String declaringClass = matcher.group(4) == null ? null : unescape(matcher.group(4));
            Instant declaredAt = parseInstant(matcher.group(5));
            Instant implementedAt = parseInstant(matcher.group(6));
            Instant stubbedAt = parseInstant(matcher.group(7));
            Instant verifiedAt = parseInstant(matcher.group(8));
            Instant lastSeenAt = parseInstant(matcher.group(9));
            Instant removedAt = parseInstant(matcher.group(10));

            Map<String, Instant> stages = new LinkedHashMap<>();
            if (declaredAt != null) {
                stages.put("declared", declaredAt);
            }
            if (implementedAt != null) {
                stages.put("implemented", implementedAt);
            }
            if (stubbedAt != null) {
                stages.put("stubbed", stubbedAt);
            }
            if (verifiedAt != null) {
                stages.put("verified", verifiedAt);
            }

            String label = verb + " " + path;
            return new LifecycleRecord(fingerprint, label, declaringClass, stages, lastSeenAt, removedAt);
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
