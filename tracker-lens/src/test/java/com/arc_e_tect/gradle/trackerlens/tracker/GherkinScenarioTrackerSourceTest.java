package com.arc_e_tect.gradle.trackerlens.tracker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GherkinScenarioTrackerSource")
class GherkinScenarioTrackerSourceTest {

    @TempDir
    Path tempDir;

    private final GherkinScenarioTrackerSource source = new GherkinScenarioTrackerSource();

    @Test
    @DisplayName("readShouldParseAllFieldsFromWellFormedLine")
    void readShouldParseAllFieldsFromWellFormedLine() throws IOException {
        Path file = writeFile(
                "{\"fingerprint\":\"abc123\",\"scenarioName\":\"Place an order\",\"featureTitle\":\"Ordering\","
                + "\"listedAt\":\"2026-01-01T00:00:00Z\",\"definedAt\":\"2026-01-02T00:00:00Z\","
                + "\"implementedAt\":null,\"lastSeenAt\":\"2026-01-10T00:00:00Z\",\"removedAt\":null}");

        List<LifecycleRecord> records = source.read(file.toFile());

        assertThat(records).containsExactly(new LifecycleRecord(
                "abc123", "Place an order", "Ordering",
                java.util.Map.of("listed", Instant.parse("2026-01-01T00:00:00Z"),
                        "defined", Instant.parse("2026-01-02T00:00:00Z")),
                Instant.parse("2026-01-10T00:00:00Z"), null));
    }

    @Test
    @DisplayName("readShouldSkipMalformedLineWithoutFailing")
    void readShouldSkipMalformedLineWithoutFailing() throws IOException {
        Path file = writeFile("not json at all");

        List<LifecycleRecord> records = source.read(file.toFile());

        assertThat(records).isEmpty();
    }

    @Test
    @DisplayName("readShouldOrderStagesByCanonicalOrderNotFileOrder")
    void readShouldOrderStagesByCanonicalOrderNotFileOrder() throws IOException {
        Path file = writeFile(
                "{\"fingerprint\":\"x\",\"scenarioName\":\"s\",\"featureTitle\":\"f\","
                + "\"listedAt\":\"2026-01-01T00:00:00Z\",\"definedAt\":\"2026-01-02T00:00:00Z\","
                + "\"implementedAt\":\"2026-01-03T00:00:00Z\",\"lastSeenAt\":\"2026-01-10T00:00:00Z\",\"removedAt\":null}");

        List<LifecycleRecord> records = source.read(file.toFile());

        assertThat(records.get(0).stageReachedAt().keySet()).containsExactly("listed", "defined", "implemented");
    }

    @Test
    @DisplayName("readShouldUnescapeQuotesAndBackslashesInFreeTextFields")
    void readShouldUnescapeQuotesAndBackslashesInFreeTextFields() throws IOException {
        Path file = writeFile(
                "{\"fingerprint\":\"y\",\"scenarioName\":\"Say \\\"hello\\\" and C:\\\\path\",\"featureTitle\":\"f\","
                + "\"listedAt\":\"2026-01-01T00:00:00Z\",\"definedAt\":null,"
                + "\"implementedAt\":null,\"lastSeenAt\":\"2026-01-10T00:00:00Z\",\"removedAt\":null}");

        List<LifecycleRecord> records = source.read(file.toFile());

        assertThat(records.get(0).label()).isEqualTo("Say \"hello\" and C:\\path");
    }

    @Test
    @DisplayName("readShouldSkipLineWithUnparseableInstant")
    void readShouldSkipLineWithUnparseableInstant() throws IOException {
        Path file = writeFile(
                "{\"fingerprint\":\"bad\",\"scenarioName\":\"s\",\"featureTitle\":\"f\","
                + "\"listedAt\":\"not-a-date\",\"definedAt\":null,"
                + "\"implementedAt\":null,\"lastSeenAt\":null,\"removedAt\":null}");

        List<LifecycleRecord> records = source.read(file.toFile());

        assertThat(records).isEmpty();
    }

    @Test
    @DisplayName("readShouldSkipBlankLines")
    void readShouldSkipBlankLines() throws IOException {
        Path file = writeFile("",
                "{\"fingerprint\":\"z\",\"scenarioName\":\"s\",\"featureTitle\":\"f\","
                + "\"listedAt\":\"2026-01-01T00:00:00Z\",\"definedAt\":null,"
                + "\"implementedAt\":null,\"lastSeenAt\":\"2026-01-10T00:00:00Z\",\"removedAt\":null}");

        List<LifecycleRecord> records = source.read(file.toFile());

        assertThat(records).hasSize(1);
    }

    private Path writeFile(String... lines) throws IOException {
        Path file = tempDir.resolve("history.ndjson");
        Files.write(file, List.of(lines));
        return file;
    }
}
