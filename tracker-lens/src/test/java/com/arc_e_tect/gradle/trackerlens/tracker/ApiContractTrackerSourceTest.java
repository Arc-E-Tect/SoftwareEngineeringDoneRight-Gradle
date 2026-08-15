package com.arc_e_tect.gradle.trackerlens.tracker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiContractTrackerSource")
class ApiContractTrackerSourceTest {

    @TempDir
    Path tempDir;

    private final ApiContractTrackerSource source = new ApiContractTrackerSource();

    @Test
    @DisplayName("readShouldParseAllFieldsFromWellFormedLine")
    void readShouldParseAllFieldsFromWellFormedLine() throws IOException {
        Path file = writeFile(
                "{\"fingerprint\":\"ep1\",\"verb\":\"GET\",\"path\":\"/orders/{id}\","
                + "\"declaringClass\":\"com.example.OrderController\","
                + "\"declaredAt\":\"2026-01-01T00:00:00Z\",\"implementedAt\":\"2026-01-02T00:00:00Z\","
                + "\"stubbedAt\":null,"
                + "\"verifiedAt\":null,\"lastSeenAt\":\"2026-01-10T00:00:00Z\",\"removedAt\":null}");

        List<LifecycleRecord> records = source.read(file.toFile());

        assertThat(records).containsExactly(new LifecycleRecord(
                "ep1", "GET /orders/{id}", "com.example.OrderController",
                Map.of("declared", Instant.parse("2026-01-01T00:00:00Z"),
                        "implemented", Instant.parse("2026-01-02T00:00:00Z")),
                Instant.parse("2026-01-10T00:00:00Z"), null));
    }

    @Test
    @DisplayName("readShouldParseStubbedAtIntoStubbedStage")
    void readShouldParseStubbedAtIntoStubbedStage() throws IOException {
        Path file = writeFile(
                "{\"fingerprint\":\"ep1b\",\"verb\":\"GET\",\"path\":\"/orders\","
                + "\"declaringClass\":null,"
                + "\"declaredAt\":\"2026-01-01T00:00:00Z\",\"implementedAt\":null,"
                + "\"stubbedAt\":\"2026-01-03T00:00:00Z\","
                + "\"verifiedAt\":null,\"lastSeenAt\":\"2026-01-10T00:00:00Z\",\"removedAt\":null}");

        List<LifecycleRecord> records = source.read(file.toFile());

        assertThat(records).containsExactly(new LifecycleRecord(
                "ep1b", "GET /orders", null,
                Map.of("declared", Instant.parse("2026-01-01T00:00:00Z"),
                        "stubbed", Instant.parse("2026-01-03T00:00:00Z")),
                Instant.parse("2026-01-10T00:00:00Z"), null));
    }

    @Test
    @DisplayName("readShouldTolerateNullDeclaringClass")
    void readShouldTolerateNullDeclaringClass() throws IOException {
        Path file = writeFile(
                "{\"fingerprint\":\"ep2\",\"verb\":\"POST\",\"path\":\"/orders\","
                + "\"declaringClass\":null,"
                + "\"declaredAt\":\"2026-01-01T00:00:00Z\",\"implementedAt\":null,"
                + "\"stubbedAt\":null,"
                + "\"verifiedAt\":null,\"lastSeenAt\":\"2026-01-01T00:00:00Z\",\"removedAt\":null}");

        List<LifecycleRecord> records = source.read(file.toFile());

        assertThat(records.get(0).group()).isNull();
    }

    @Test
    @DisplayName("readShouldSkipMalformedLineWithoutFailing")
    void readShouldSkipMalformedLineWithoutFailing() throws IOException {
        Path file = writeFile("{\"broken\":true}");

        List<LifecycleRecord> records = source.read(file.toFile());

        assertThat(records).isEmpty();
    }

    @Test
    @DisplayName("readShouldSkipLegacyNineFieldLineWithoutStubbedAt")
    void readShouldSkipLegacyNineFieldLineWithoutStubbedAt() throws IOException {
        Path file = writeFile(
                "{\"fingerprint\":\"legacy\",\"verb\":\"GET\",\"path\":\"/orders\","
                + "\"declaringClass\":null,"
                + "\"declaredAt\":\"2026-01-01T00:00:00Z\",\"implementedAt\":null,"
                + "\"verifiedAt\":null,\"lastSeenAt\":null,\"removedAt\":null}");

        List<LifecycleRecord> records = source.read(file.toFile());

        assertThat(records).isEmpty();
    }

    @Test
    @DisplayName("readShouldUnescapeQuotesAndBackslashesInPath")
    void readShouldUnescapeQuotesAndBackslashesInPath() throws IOException {
        Path file = writeFile(
                "{\"fingerprint\":\"ep3\",\"verb\":\"GET\",\"path\":\"/say \\\"hi\\\" C:\\\\path\","
                + "\"declaringClass\":null,"
                + "\"declaredAt\":\"2026-01-01T00:00:00Z\",\"implementedAt\":null,"
                + "\"stubbedAt\":null,"
                + "\"verifiedAt\":null,\"lastSeenAt\":null,\"removedAt\":null}");

        List<LifecycleRecord> records = source.read(file.toFile());

        assertThat(records.get(0).label()).isEqualTo("GET /say \"hi\" C:\\path");
    }

    @Test
    @DisplayName("readShouldSkipLineWithUnparseableInstant")
    void readShouldSkipLineWithUnparseableInstant() throws IOException {
        Path file = writeFile(
                "{\"fingerprint\":\"bad\",\"verb\":\"GET\",\"path\":\"/orders\","
                + "\"declaringClass\":null,"
                + "\"declaredAt\":\"not-a-date\",\"implementedAt\":null,"
                + "\"stubbedAt\":null,"
                + "\"verifiedAt\":null,\"lastSeenAt\":null,\"removedAt\":null}");

        List<LifecycleRecord> records = source.read(file.toFile());

        assertThat(records).isEmpty();
    }

    @Test
    @DisplayName("readShouldSkipBlankLines")
    void readShouldSkipBlankLines() throws IOException {
        Path file = writeFile("",
                "{\"fingerprint\":\"ep4\",\"verb\":\"GET\",\"path\":\"/orders\","
                + "\"declaringClass\":null,"
                + "\"declaredAt\":\"2026-01-01T00:00:00Z\",\"implementedAt\":null,"
                + "\"stubbedAt\":null,"
                + "\"verifiedAt\":null,\"lastSeenAt\":null,\"removedAt\":null}");

        List<LifecycleRecord> records = source.read(file.toFile());

        assertThat(records).hasSize(1);
    }

    private Path writeFile(String... lines) throws IOException {
        Path file = tempDir.resolve("history.ndjson");
        Files.write(file, List.of(lines));
        return file;
    }
}
