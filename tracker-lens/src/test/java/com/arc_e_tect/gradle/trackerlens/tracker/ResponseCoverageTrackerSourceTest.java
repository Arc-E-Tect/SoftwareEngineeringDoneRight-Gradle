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

@DisplayName("ResponseCoverageTrackerSource")
class ResponseCoverageTrackerSourceTest {

    @TempDir
    Path tempDir;

    private final ResponseCoverageTrackerSource source = new ResponseCoverageTrackerSource();

    @Test
    @DisplayName("readShouldParseAllFieldsFromWellFormedLine")
    void readShouldParseAllFieldsFromWellFormedLine() throws IOException {
        Path file = writeFile(
                "{\"fingerprint\":\"fp1-200\",\"verb\":\"GET\",\"path\":\"/v1/foobars\","
                + "\"responseCode\":\"200\",\"testCount\":2,"
                + "\"firstDeclaredAt\":\"2026-01-01T00:00:00Z\",\"firstCoveredAt\":\"2026-01-05T00:00:00Z\","
                + "\"lastSeenAt\":\"2026-01-10T00:00:00Z\",\"removedAt\":null}");

        List<LifecycleRecord> records = source.read(file.toFile());

        assertThat(records).containsExactly(new LifecycleRecord(
                "fp1-200", "GET /v1/foobars 200", "2xx",
                Map.of("declared", Instant.parse("2026-01-01T00:00:00Z"),
                        "covered", Instant.parse("2026-01-05T00:00:00Z")),
                Instant.parse("2026-01-10T00:00:00Z"), null));
    }

    @Test
    @DisplayName("readShouldOmitCoveredStageWhenFirstCoveredAtIsNull")
    void readShouldOmitCoveredStageWhenFirstCoveredAtIsNull() throws IOException {
        Path file = writeFile(
                "{\"fingerprint\":\"fp1-404\",\"verb\":\"GET\",\"path\":\"/v1/foobars\","
                + "\"responseCode\":\"404\",\"testCount\":0,"
                + "\"firstDeclaredAt\":\"2026-01-01T00:00:00Z\",\"firstCoveredAt\":null,"
                + "\"lastSeenAt\":\"2026-01-10T00:00:00Z\",\"removedAt\":null}");

        List<LifecycleRecord> records = source.read(file.toFile());

        assertThat(records.get(0).hasReached("covered")).isFalse();
        assertThat(records.get(0).hasReached("declared")).isTrue();
    }

    @Test
    @DisplayName("readShouldGroupByResponseCodeClass")
    void readShouldGroupByResponseCodeClass() throws IOException {
        Path file = writeFile(
                "{\"fingerprint\":\"a-200\",\"verb\":\"GET\",\"path\":\"/a\",\"responseCode\":\"200\",\"testCount\":1,"
                + "\"firstDeclaredAt\":\"2026-01-01T00:00:00Z\",\"firstCoveredAt\":null,"
                + "\"lastSeenAt\":null,\"removedAt\":null}",
                "{\"fingerprint\":\"a-404\",\"verb\":\"GET\",\"path\":\"/a\",\"responseCode\":\"404\",\"testCount\":0,"
                + "\"firstDeclaredAt\":\"2026-01-01T00:00:00Z\",\"firstCoveredAt\":null,"
                + "\"lastSeenAt\":null,\"removedAt\":null}",
                "{\"fingerprint\":\"a-500\",\"verb\":\"GET\",\"path\":\"/a\",\"responseCode\":\"500\",\"testCount\":0,"
                + "\"firstDeclaredAt\":\"2026-01-01T00:00:00Z\",\"firstCoveredAt\":null,"
                + "\"lastSeenAt\":null,\"removedAt\":null}",
                "{\"fingerprint\":\"a-default\",\"verb\":\"GET\",\"path\":\"/a\",\"responseCode\":\"default\","
                + "\"testCount\":0,\"firstDeclaredAt\":\"2026-01-01T00:00:00Z\",\"firstCoveredAt\":null,"
                + "\"lastSeenAt\":null,\"removedAt\":null}");

        List<LifecycleRecord> records = source.read(file.toFile());

        assertThat(records).extracting(LifecycleRecord::group)
                .containsExactly("2xx", "4xx", "5xx", "other");
    }

    @Test
    @DisplayName("readShouldSkipMalformedLineWithoutFailing")
    void readShouldSkipMalformedLineWithoutFailing() throws IOException {
        Path file = writeFile("{\"broken\":true}");

        List<LifecycleRecord> records = source.read(file.toFile());

        assertThat(records).isEmpty();
    }

    @Test
    @DisplayName("readShouldSkipLineWithUnparseableInstant")
    void readShouldSkipLineWithUnparseableInstant() throws IOException {
        Path file = writeFile(
                "{\"fingerprint\":\"bad-200\",\"verb\":\"GET\",\"path\":\"/a\",\"responseCode\":\"200\","
                + "\"testCount\":0,\"firstDeclaredAt\":\"not-a-date\",\"firstCoveredAt\":null,"
                + "\"lastSeenAt\":null,\"removedAt\":null}");

        List<LifecycleRecord> records = source.read(file.toFile());

        assertThat(records).isEmpty();
    }

    @Test
    @DisplayName("readShouldSkipBlankLines")
    void readShouldSkipBlankLines() throws IOException {
        Path file = writeFile("",
                "{\"fingerprint\":\"fp-200\",\"verb\":\"GET\",\"path\":\"/a\",\"responseCode\":\"200\","
                + "\"testCount\":0,\"firstDeclaredAt\":\"2026-01-01T00:00:00Z\",\"firstCoveredAt\":null,"
                + "\"lastSeenAt\":null,\"removedAt\":null}");

        List<LifecycleRecord> records = source.read(file.toFile());

        assertThat(records).hasSize(1);
    }

    @Test
    @DisplayName("readShouldSkipALeadingSchemaVersionMarkerLineWithoutLoggingItAsMalformed")
    void readShouldSkipALeadingSchemaVersionMarkerLineWithoutLoggingItAsMalformed() throws IOException {
        Path file = writeFile(
                "{\"schemaVersion\":1}",
                "{\"fingerprint\":\"fp-200\",\"verb\":\"GET\",\"path\":\"/a\",\"responseCode\":\"200\","
                + "\"testCount\":0,\"firstDeclaredAt\":\"2026-01-01T00:00:00Z\",\"firstCoveredAt\":null,"
                + "\"lastSeenAt\":null,\"removedAt\":null}");

        List<LifecycleRecord> records = source.read(file.toFile());

        assertThat(records).extracting(LifecycleRecord::id).containsExactly("fp-200");
    }

    @Test
    @DisplayName("responseCodeClassShouldBucketByFirstDigit")
    void responseCodeClassShouldBucketByFirstDigit() {
        assertThat(ResponseCoverageTrackerSource.responseCodeClass("200")).isEqualTo("2xx");
        assertThat(ResponseCoverageTrackerSource.responseCodeClass("301")).isEqualTo("3xx");
        assertThat(ResponseCoverageTrackerSource.responseCodeClass("404")).isEqualTo("4xx");
        assertThat(ResponseCoverageTrackerSource.responseCodeClass("503")).isEqualTo("5xx");
        assertThat(ResponseCoverageTrackerSource.responseCodeClass("5XX")).isEqualTo("5xx");
        assertThat(ResponseCoverageTrackerSource.responseCodeClass("default")).isEqualTo("other");
        assertThat(ResponseCoverageTrackerSource.responseCodeClass("")).isEqualTo("other");
    }

    private Path writeFile(String... lines) throws IOException {
        Path file = tempDir.resolve("history.ndjson");
        Files.write(file, List.of(lines));
        return file;
    }
}
