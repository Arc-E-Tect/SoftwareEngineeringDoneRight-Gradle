package com.arc_e_tect.gradle.doppelganger.report;

import com.arc_e_tect.gradle.doppelganger.detect.EndpointResponseCoverage;
import com.arc_e_tect.gradle.doppelganger.progress.ResponseCoverageRecord;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Writes the AsciiDoc {@code scanContracts} report: for every endpoint both declared in the
 * OpenAPI documentation and implemented by a {@code @RestController} method, its declared response
 * codes and contract test count, plus - when {@code includeResponseCoverage} was enabled - a
 * per-response-code test count breakdown.
 */
public class ScanContractsReportWriter {

    /** Classpath resource holding the "what does this report show" preamble, bundled with the plugin. */
    static final String PREAMBLE_RESOURCE = "scan-contracts-preamble.adoc";

    private final ResponseCoverageTableWriter historyTableWriter = new ResponseCoverageTableWriter();

    /** Creates a new {@code ScanContractsReportWriter}. */
    public ScanContractsReportWriter() {}

    /**
     * Writes the report to {@code outputFile}, creating its parent directory if necessary.
     *
     * @param outputFile              target AsciiDoc file
     * @param coverage                the coverage rows to render, one per declared-and-implemented
     *                                 endpoint
     * @param includeResponseCoverage whether {@code coverage}'s per-response-code breakdown was
     *                                 computed; when {@code false}, only the declared response code
     *                                 count and contract test count are rendered
     * @param systemUnderTestVersion  version of the system under test that was scanned
     * @param warnings                non-fatal configuration gaps to render as a {@code WARNING}
     *                                 admonition right after the report header; when empty, no such
     *                                 admonition is written
     * @param responseCoverageHistory response coverage history to render as a
     *                                 {@code == Response Coverage Over Time} section, keyed by
     *                                 fingerprint; when empty, no such section is written
     * @throws IOException if the output file cannot be written
     */
    public void write(
            File outputFile, List<EndpointResponseCoverage> coverage, boolean includeResponseCoverage,
            String systemUnderTestVersion, List<String> warnings,
            Map<String, ResponseCoverageRecord> responseCoverageHistory) throws IOException {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create output directory: " + parent);
        }

        try (PrintWriter writer = new PrintWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.println("= Contract Scan Report");
            writer.println(":toc:");
            writer.println(":toclevels: 2");
            writer.println();
            writer.println("System Under Test version: " + systemUnderTestVersion);
            writer.println();
            writer.println("Generated: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println();
            writeWarnings(writer, warnings);
            writer.println("Scanned " + coverage.size()
                    + " endpoint(s) both declared in the OpenAPI documentation and implemented by a "
                    + "`@RestController` class.");
            writer.println();
            writer.print(loadPreamble());
            writer.println();
            historyTableWriter.write(writer, responseCoverageHistory);
            writer.println("== Endpoint Coverage");
            writer.println();

            if (coverage.isEmpty()) {
                writer.println("None found - no endpoint is both declared in the OpenAPI documentation and "
                        + "implemented by a `@RestController` class.");
            } else {
                writeCoverageTable(writer, coverage, includeResponseCoverage);
            }
        }
    }

    private void writeCoverageTable(
            PrintWriter writer, List<EndpointResponseCoverage> coverage, boolean includeResponseCoverage) {
        List<EndpointResponseCoverage> sorted = coverage.stream()
                .sorted(Comparator.<EndpointResponseCoverage, String>comparing(row -> row.endpoint().path())
                        .thenComparing(row -> row.endpoint().verb().name()))
                .toList();

        writer.println("[cols=\"1,3,1,1\",options=\"header\"]");
        writer.println("|===");
        writer.println("| HTTP Verb | Path | Declared Response Codes | Contract Test Count");

        for (EndpointResponseCoverage row : sorted) {
            writer.println();
            writer.println("| " + row.endpoint().verb());
            writer.println("| " + row.endpoint().path());
            writer.println("| " + row.declaredResponseCodes().size() + " (" + String.join(", ", row.declaredResponseCodes()) + ")");
            writer.println("| " + row.contractTestCount());
        }
        writer.println("|===");
        writer.println();

        if (includeResponseCoverage) {
            writeResponseCoverageBreakdown(writer, sorted);
        }
    }

    private void writeResponseCoverageBreakdown(PrintWriter writer, List<EndpointResponseCoverage> sorted) {
        writer.println("== Response Code Coverage");
        writer.println();

        for (EndpointResponseCoverage row : sorted) {
            if (row.declaredResponseCodes().isEmpty()) {
                continue;
            }
            writer.println("=== " + row.endpoint().verb() + " " + row.endpoint().path());
            writer.println();
            writer.println("[cols=\"1,1\",options=\"header\"]");
            writer.println("|===");
            writer.println("| Response Code | Contract Test Count");

            for (Map.Entry<String, Integer> entry : row.testCountByResponseCode().entrySet()) {
                writer.println();
                writer.println("| " + entry.getKey());
                writer.println("| " + entry.getValue());
            }
            writer.println("|===");
            writer.println();

            if (row.untrackedTestCount() > 0) {
                writer.println(row.untrackedTestCount()
                        + (row.untrackedTestCount() == 1 ? " test asserts" : " tests assert")
                        + " a response status that could not be matched to a declared response code (or could "
                        + "not be detected at all), and " + (row.untrackedTestCount() == 1 ? "is" : "are") + " "
                        + "not reflected above.");
                writer.println();
            }
        }
    }

    private void writeWarnings(PrintWriter writer, List<String> warnings) {
        if (warnings.isEmpty()) {
            return;
        }
        writer.println("[WARNING]");
        writer.println("====");
        for (String warning : warnings) {
            writer.println("* " + warning);
        }
        writer.println("====");
        writer.println();
    }

    private String loadPreamble() throws IOException {
        try (InputStream stream = ScanContractsReportWriter.class.getClassLoader()
                .getResourceAsStream(PREAMBLE_RESOURCE)) {
            if (stream == null) {
                throw new IOException("Missing bundled resource: " + PREAMBLE_RESOURCE);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
