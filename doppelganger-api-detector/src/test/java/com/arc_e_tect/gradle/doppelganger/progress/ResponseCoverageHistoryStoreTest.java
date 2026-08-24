package com.arc_e_tect.gradle.doppelganger.progress;

import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
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

@DisplayName("ResponseCoverageHistoryStore")
class ResponseCoverageHistoryStoreTest {

    @TempDir
    Path tempDir;

    private final ResponseCoverageHistoryStore store = new ResponseCoverageHistoryStore();

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
        ResponseCoverageRecord record = new ResponseCoverageRecord(
                "3c7a1f0e9b224dd1-200", HttpVerb.GET, "/v1/foobars", "200", 2,
                Instant.parse("2026-01-10T09:00:00Z"),
                Instant.parse("2026-01-15T10:00:00Z"),
                Instant.parse("2026-08-12T07:00:00Z"),
                null);

        store.save(file, List.of(record));
        Map<String, ResponseCoverageRecord> loaded = store.load(file);

        assertThat(loaded.get("3c7a1f0e9b224dd1-200")).isEqualTo(record);
    }

    @Test
    @DisplayName("round-trips a null firstCoveredAt and removedAt")
    void roundTripsNullInstants() {
        File file = tempDir.resolve("history.ndjson").toFile();
        ResponseCoverageRecord record = new ResponseCoverageRecord(
                "3c7a1f0e9b224dd1-404", HttpVerb.GET, "/v1/foobars", "404", 0,
                Instant.parse("2026-01-10T09:00:00Z"), null, Instant.parse("2026-08-12T07:00:00Z"), null);

        store.save(file, List.of(record));
        Map<String, ResponseCoverageRecord> loaded = store.load(file);

        assertThat(loaded.get("3c7a1f0e9b224dd1-404").firstCoveredAt()).isNull();
        assertThat(loaded.get("3c7a1f0e9b224dd1-404").removedAt()).isNull();
    }

    @Test
    @DisplayName("writes a schemaVersion marker as the file's first line")
    void writesSchemaVersionMarkerAsFirstLine() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();

        store.save(file, List.of());

        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        assertThat(lines.get(0)).isEqualTo("{\"schemaVersion\":1}");
    }

    @Test
    @DisplayName("writes records sorted by fingerprint")
    void writesRecordsSortedByFingerprint() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        ResponseCoverageRecord b = new ResponseCoverageRecord(
                "b-fingerprint", HttpVerb.GET, "/b", "200", 1, Instant.now(), null, Instant.now(), null);
        ResponseCoverageRecord a = new ResponseCoverageRecord(
                "a-fingerprint", HttpVerb.GET, "/a", "200", 1, Instant.now(), null, Instant.now(), null);

        store.save(file, List.of(b, a));

        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        assertThat(lines.get(1)).contains("a-fingerprint");
        assertThat(lines.get(2)).contains("b-fingerprint");
    }

    @Test
    @DisplayName("skips a malformed line without failing the build")
    void skipsMalformedLine() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        Files.writeString(file.toPath(), "{\"schemaVersion\":1}\nnot json at all\n");

        assertThat(store.load(file)).isEmpty();
    }

    @Test
    @DisplayName("tolerates a file with no schemaVersion marker")
    void toleratesFileWithNoSchemaVersionMarker() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        Files.writeString(file.toPath(), "{\"fingerprint\":\"abc-200\",\"verb\":\"GET\",\"path\":\"/x\","
                + "\"responseCode\":\"200\",\"testCount\":1,\"firstDeclaredAt\":\"2026-01-01T00:00:00Z\","
                + "\"firstCoveredAt\":null,\"lastSeenAt\":\"2026-01-01T00:00:00Z\",\"removedAt\":null}\n");

        Map<String, ResponseCoverageRecord> loaded = store.load(file);

        assertThat(loaded).containsKey("abc-200");
    }
}
