package com.arc_e_tect.gradle.trackerlens.fixture;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The in-memory result of {@link TrackerLensFixtureGenerator#generate(FixtureSpec)}: both
 * trackers' generated records, plus a way to write them to disk.
 *
 * <p>Generating in memory first and writing second - rather than only ever writing straight to
 * disk - is what lets an automated test consume {@link #bddScenarios()} and
 * {@link #apiContracts()} directly, with no filesystem round trip at all.</p>
 *
 * @param bddScenarios the generated BDD-scenario records
 * @param apiContracts the generated API-contract records
 */
public record FixtureResult(
        List<BddScenarioFixtureRecord> bddScenarios,
        List<ApiContractFixtureRecord> apiContracts) {

    /** Defensively copies both lists into unmodifiable ones. */
    public FixtureResult {
        bddScenarios = List.copyOf(bddScenarios);
        apiContracts = List.copyOf(apiContracts);
    }

    /**
     * Writes both trackers' records to {@code bddScenarioHistoryFile} and
     * {@code apiContractHistoryFile}, each in the exact NDJSON format its real
     * {@code TrackerSource} reads - {@code gherkin-to-asciidoc}'s {@code ProgressHistoryStore} and
     * {@code api-detector-core}'s {@code ContractHistoryStore} shapes respectively, schema-version
     * marker line included, sorted by fingerprint. The written files are ordinary history files,
     * indistinguishable from real usage data - there is no fixture marker in the format itself.
     *
     * @param bddScenarioHistoryFile the file to write {@link #bddScenarios()} to
     * @param apiContractHistoryFile the file to write {@link #apiContracts()} to
     */
    public void writeTo(File bddScenarioHistoryFile, File apiContractHistoryFile) {
        writeBddScenarios(bddScenarioHistoryFile);
        writeApiContracts(apiContractHistoryFile);
    }

    private void writeBddScenarios(File file) {
        List<BddScenarioFixtureRecord> sorted = new ArrayList<>(bddScenarios);
        sorted.sort(Comparator.comparing(BddScenarioFixtureRecord::fingerprint));

        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.println("{\"schemaVersion\":1}");
            for (BddScenarioFixtureRecord record : sorted) {
                writer.println("{"
                        + "\"fingerprint\":\"" + record.fingerprint() + "\","
                        + "\"scenarioName\":\"" + escape(record.scenarioName()) + "\","
                        + "\"featureTitle\":\"" + escape(record.featureTitle()) + "\","
                        + "\"listedAt\":" + instantJson(record.listedAt()) + ","
                        + "\"definedAt\":" + instantJson(record.definedAt()) + ","
                        + "\"implementedAt\":" + instantJson(record.implementedAt()) + ","
                        + "\"lastSeenAt\":" + instantJson(record.lastSeenAt()) + ","
                        + "\"removedAt\":" + instantJson(record.removedAt())
                        + "}");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("trackerLensFixture: could not write BDD scenario history file: " + file, e);
        }
    }

    private void writeApiContracts(File file) {
        List<ApiContractFixtureRecord> sorted = new ArrayList<>(apiContracts);
        sorted.sort(Comparator.comparing(ApiContractFixtureRecord::fingerprint));

        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.println("{\"schemaVersion\":1}");
            for (ApiContractFixtureRecord record : sorted) {
                writer.println("{"
                        + "\"fingerprint\":\"" + record.fingerprint() + "\","
                        + "\"verb\":\"" + record.verb() + "\","
                        + "\"path\":\"" + escape(record.path()) + "\","
                        + "\"declaringClass\":" + nullableStringJson(record.declaringClass()) + ","
                        + "\"declaredAt\":" + instantJson(record.declaredAt()) + ","
                        + "\"implementedAt\":" + instantJson(record.implementedAt()) + ","
                        + "\"stubbedAt\":" + instantJson(record.stubbedAt()) + ","
                        + "\"verifiedAt\":" + instantJson(record.verifiedAt()) + ","
                        + "\"lastSeenAt\":" + instantJson(record.lastSeenAt()) + ","
                        + "\"removedAt\":" + instantJson(record.removedAt())
                        + "}");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("trackerLensFixture: could not write API contract history file: " + file, e);
        }
    }

    private String nullableStringJson(String value) {
        return value == null ? "null" : "\"" + escape(value) + "\"";
    }

    private String instantJson(Instant instant) {
        return instant == null ? "null" : "\"" + instant + "\"";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
