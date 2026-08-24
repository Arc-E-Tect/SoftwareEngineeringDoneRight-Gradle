package com.arc_e_tect.gradle.trackerlens.tracker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResponseCoverageMatrixReader")
class ResponseCoverageMatrixReaderTest {

    @TempDir
    Path tempDir;

    private final ResponseCoverageMatrixReader reader = new ResponseCoverageMatrixReader();

    @Test
    @DisplayName("readShouldParseVerbPathResponseCodeAndTestCount")
    void readShouldParseVerbPathResponseCodeAndTestCount() throws IOException {
        Path file = writeFile(
                "{\"fingerprint\":\"fp1-200\",\"verb\":\"GET\",\"path\":\"/v1/foobars\","
                + "\"responseCode\":\"200\",\"testCount\":2,"
                + "\"firstDeclaredAt\":\"2026-01-01T00:00:00Z\",\"firstCoveredAt\":\"2026-01-05T00:00:00Z\","
                + "\"lastSeenAt\":\"2026-01-10T00:00:00Z\",\"removedAt\":null}");

        List<ResponseCoverageCell> cells = reader.read(file.toFile());

        assertThat(cells).containsExactly(new ResponseCoverageCell("GET", "/v1/foobars", "200", 2, true));
    }

    @Test
    @DisplayName("readShouldMarkZeroTestCountAsNotCovered")
    void readShouldMarkZeroTestCountAsNotCovered() throws IOException {
        Path file = writeFile(
                "{\"fingerprint\":\"fp1-404\",\"verb\":\"GET\",\"path\":\"/v1/foobars\","
                + "\"responseCode\":\"404\",\"testCount\":0,"
                + "\"firstDeclaredAt\":\"2026-01-01T00:00:00Z\",\"firstCoveredAt\":null,"
                + "\"lastSeenAt\":\"2026-01-10T00:00:00Z\",\"removedAt\":null}");

        List<ResponseCoverageCell> cells = reader.read(file.toFile());

        assertThat(cells).containsExactly(new ResponseCoverageCell("GET", "/v1/foobars", "404", 0, false));
    }

    @Test
    @DisplayName("readShouldExcludeCellsWithNonNullRemovedAt")
    void readShouldExcludeCellsWithNonNullRemovedAt() throws IOException {
        Path file = writeFile(
                "{\"fingerprint\":\"fp1-410\",\"verb\":\"GET\",\"path\":\"/v1/foobars\","
                + "\"responseCode\":\"410\",\"testCount\":1,"
                + "\"firstDeclaredAt\":\"2026-01-01T00:00:00Z\",\"firstCoveredAt\":\"2026-01-02T00:00:00Z\","
                + "\"lastSeenAt\":\"2026-01-05T00:00:00Z\",\"removedAt\":\"2026-01-06T00:00:00Z\"}");

        List<ResponseCoverageCell> cells = reader.read(file.toFile());

        assertThat(cells).isEmpty();
    }

    @Test
    @DisplayName("readShouldSkipMalformedLineWithoutFailing")
    void readShouldSkipMalformedLineWithoutFailing() throws IOException {
        Path file = writeFile("{\"broken\":true}");

        assertThat(reader.read(file.toFile())).isEmpty();
    }

    @Test
    @DisplayName("readShouldSkipALeadingSchemaVersionMarkerLineWithoutLoggingItAsMalformed")
    void readShouldSkipALeadingSchemaVersionMarkerLineWithoutLoggingItAsMalformed() throws IOException {
        Path file = writeFile(
                "{\"schemaVersion\":1}",
                "{\"fingerprint\":\"fp1-200\",\"verb\":\"GET\",\"path\":\"/v1/foobars\","
                + "\"responseCode\":\"200\",\"testCount\":1,"
                + "\"firstDeclaredAt\":\"2026-01-01T00:00:00Z\",\"firstCoveredAt\":\"2026-01-02T00:00:00Z\","
                + "\"lastSeenAt\":null,\"removedAt\":null}");

        List<ResponseCoverageCell> cells = reader.read(file.toFile());

        assertThat(cells).hasSize(1);
    }

    private Path writeFile(String... lines) throws IOException {
        Path file = tempDir.resolve("history.ndjson");
        Files.write(file, List.of(lines));
        return file;
    }
}
