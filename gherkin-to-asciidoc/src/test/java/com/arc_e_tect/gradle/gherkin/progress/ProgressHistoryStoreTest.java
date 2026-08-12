package com.arc_e_tect.gradle.gherkin.progress;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProgressHistoryStore")
class ProgressHistoryStoreTest {

    @TempDir
    Path tempDir;

    private final ProgressHistoryStore store = new ProgressHistoryStore();

    @Test
    @DisplayName("load returns an empty map when the file does not exist")
    void loadReturnsEmptyMapWhenFileDoesNotExist() {
        File missingFile = tempDir.resolve("missing.ndjson").toFile();

        assertThat(store.load(missingFile)).isEmpty();
    }

    @Test
    @DisplayName("round-trips a record's every field through save then load")
    void roundTripsRecordFieldsThroughSaveThenLoad() {
        File file = tempDir.resolve("history.ndjson").toFile();
        ScenarioProgressRecord record = new ScenarioProgressRecord(
                "a1f3c9d0e21b7f44", "User logs in", "User authentication",
                Instant.parse("2026-01-14T09:02:11Z"),
                Instant.parse("2026-02-03T16:40:00Z"),
                Instant.parse("2026-02-20T11:15:44Z"),
                Instant.parse("2026-08-12T07:00:00Z"),
                null);

        store.save(file, List.of(record));
        Map<String, ScenarioProgressRecord> loaded = store.load(file);

        assertThat(loaded.get("a1f3c9d0e21b7f44")).isEqualTo(record);
    }

    @Test
    @DisplayName("round-trips scenario names containing quotes and backslashes")
    void roundTripsScenarioNamesContainingQuotesAndBackslashes() {
        File file = tempDir.resolve("history.ndjson").toFile();
        ScenarioProgressRecord record = new ScenarioProgressRecord(
                "abc0000000000000", "User enters \"quoted\" text and a \\backslash\\", "A feature",
                Instant.parse("2026-01-01T00:00:00Z"), null, null, Instant.parse("2026-01-01T00:00:00Z"), null);

        store.save(file, List.of(record));
        Map<String, ScenarioProgressRecord> loaded = store.load(file);

        assertThat(loaded.get("abc0000000000000").scenarioName())
                .isEqualTo("User enters \"quoted\" text and a \\backslash\\");
    }

    @Test
    @DisplayName("writes records sorted by fingerprint regardless of input order")
    void writesRecordsSortedByFingerprintRegardlessOfInputOrder() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        ScenarioProgressRecord second = new ScenarioProgressRecord(
                "bbbb000000000000", "B", "Feature", Instant.parse("2026-01-01T00:00:00Z"),
                null, null, Instant.parse("2026-01-01T00:00:00Z"), null);
        ScenarioProgressRecord first = new ScenarioProgressRecord(
                "aaaa000000000000", "A", "Feature", Instant.parse("2026-01-01T00:00:00Z"),
                null, null, Instant.parse("2026-01-01T00:00:00Z"), null);

        store.save(file, List.of(second, first));

        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        assertThat(lines.get(0)).contains("\"aaaa000000000000\"");
        assertThat(lines.get(1)).contains("\"bbbb000000000000\"");
    }

    @Test
    @DisplayName("skips a malformed line and logs its line number, without failing")
    void skipsMalformedLineWithoutFailing() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        String validLine = "{\"fingerprint\":\"aaaa000000000000\",\"scenarioName\":\"A\","
                + "\"featureTitle\":\"Feature\",\"listedAt\":\"2026-01-01T00:00:00Z\","
                + "\"definedAt\":null,\"implementedAt\":null,\"lastSeenAt\":\"2026-01-01T00:00:00Z\","
                + "\"removedAt\":null}";
        Files.writeString(file.toPath(), "not valid json at all\n" + validLine + "\n", StandardCharsets.UTF_8);

        Map<String, ScenarioProgressRecord> loaded = store.load(file);

        assertThat(loaded).containsOnlyKeys("aaaa000000000000");
    }

    @Test
    @DisplayName("blank lines in the file are silently skipped")
    void blankLinesAreSilentlySkipped() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        String validLine = "{\"fingerprint\":\"aaaa000000000000\",\"scenarioName\":\"A\","
                + "\"featureTitle\":\"Feature\",\"listedAt\":\"2026-01-01T00:00:00Z\","
                + "\"definedAt\":null,\"implementedAt\":null,\"lastSeenAt\":\"2026-01-01T00:00:00Z\","
                + "\"removedAt\":null}";
        Files.writeString(file.toPath(), "\n" + validLine + "\n\n", StandardCharsets.UTF_8);

        Map<String, ScenarioProgressRecord> loaded = store.load(file);

        assertThat(loaded).hasSize(1);
    }
}
