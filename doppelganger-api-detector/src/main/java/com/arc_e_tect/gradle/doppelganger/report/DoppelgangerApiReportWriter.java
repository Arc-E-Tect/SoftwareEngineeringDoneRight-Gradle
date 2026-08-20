package com.arc_e_tect.gradle.doppelganger.report;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.progress.ContractProgressRecord;
import com.arc_e_tect.gradle.detector.core.progress.ContractProgressTableWriter;

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
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Writes the AsciiDoc doppelganger API report: every endpoint that is declared in the OpenAPI
 * documentation and implemented by a {@code @RestController} method, but has no verification
 * evidence from any enabled {@link com.arc_e_tect.gradle.doppelganger.detect.ContractVerificationSource}.
 *
 * <p>Every report opens with a preamble explaining what a doppelganger API is, loaded from the
 * {@value #PREAMBLE_RESOURCE} classpath resource so that explanatory text can be revised without
 * touching this class.</p>
 */
public class DoppelgangerApiReportWriter {

    /** Classpath resource holding the "what is a doppelganger API" preamble, bundled with the plugin. */
    static final String PREAMBLE_RESOURCE = "doppelganger-api-preamble.adoc";

    private final ContractProgressTableWriter progressTableWriter = new ContractProgressTableWriter();

    /** Creates a new {@code DoppelgangerApiReportWriter}. */
    public DoppelgangerApiReportWriter() {}

    /**
     * Writes the report to {@code outputFile}, creating its parent directory if necessary.
     * Equivalent to {@link #write(File, int, List, String, List, Map)} with no warnings and an empty
     * contract history.
     *
     * @param outputFile             target AsciiDoc file
     * @param totalCandidateCount    total number of endpoints both declared and implemented, i.e.
     *                                the candidate pool checked for verification evidence
     * @param doppelgangers          the candidate endpoints with no verification evidence
     * @param systemUnderTestVersion version of the system under test that was scanned
     * @throws IOException if the output file cannot be written
     */
    public void write(
            File outputFile, int totalCandidateCount, List<Endpoint> doppelgangers,
            String systemUnderTestVersion) throws IOException {
        write(outputFile, totalCandidateCount, doppelgangers, systemUnderTestVersion, List.of(), Map.of());
    }

    /**
     * Writes the report to {@code outputFile}, creating its parent directory if necessary.
     * Equivalent to {@link #write(File, int, List, String, List, Map)} with no warnings.
     *
     * @param outputFile             target AsciiDoc file
     * @param totalCandidateCount    total number of endpoints both declared and implemented, i.e.
     *                                the candidate pool checked for verification evidence
     * @param doppelgangers          the candidate endpoints with no verification evidence
     * @param systemUnderTestVersion version of the system under test that was scanned
     * @param contractHistory        contract progress history to render as a {@code == Progress Over
     *                                Time} section, keyed by fingerprint; when empty, no such
     *                                section is written
     * @throws IOException if the output file cannot be written
     */
    public void write(
            File outputFile, int totalCandidateCount, List<Endpoint> doppelgangers,
            String systemUnderTestVersion, Map<String, ContractProgressRecord> contractHistory) throws IOException {
        write(outputFile, totalCandidateCount, doppelgangers, systemUnderTestVersion, List.of(), contractHistory);
    }

    /**
     * Writes the report to {@code outputFile}, creating its parent directory if necessary.
     *
     * @param outputFile             target AsciiDoc file
     * @param totalCandidateCount    total number of endpoints both declared and implemented, i.e.
     *                                the candidate pool checked for verification evidence
     * @param doppelgangers          the candidate endpoints with no verification evidence
     * @param systemUnderTestVersion version of the system under test that was scanned
     * @param warnings               non-fatal configuration gaps to render as a {@code WARNING}
     *                                admonition right after the report header - e.g. a configured
     *                                {@code rootDocument}, {@code controllerDirs}, {@code testDirs},
     *                                or {@code contractsDir} entry that doesn't exist yet; when
     *                                empty, no such admonition is written
     * @param contractHistory        contract progress history to render as a {@code == Progress Over
     *                                Time} section, keyed by fingerprint; when empty, no such
     *                                section is written
     * @throws IOException if the output file cannot be written
     */
    public void write(
            File outputFile, int totalCandidateCount, List<Endpoint> doppelgangers,
            String systemUnderTestVersion, List<String> warnings, Map<String, ContractProgressRecord> contractHistory)
            throws IOException {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create output directory: " + parent);
        }

        try (PrintWriter writer = new PrintWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.println("= Doppelganger API Report");
            writer.println(":toc:");
            writer.println(":toclevels: 2");
            writer.println();
            writer.println("System Under Test version: " + systemUnderTestVersion);
            writer.println();
            writer.println("Generated: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println();
            writeWarnings(writer, warnings);
            writer.println("Scanned " + totalCandidateCount
                    + " endpoint(s) both declared in the OpenAPI documentation and implemented by a "
                    + "`@RestController` class. " + doppelgangers.size()
                    + (doppelgangers.size() == 1 ? " of them is" : " of them are")
                    + " not verified by any configured contract verification source.");
            writer.println();
            writer.print(loadPreamble());
            writer.println();
            progressTableWriter.write(writer, contractHistory);
            writer.println("== Doppelganger APIs");
            writer.println();

            if (doppelgangers.isEmpty()) {
                writer.println("None found. Every endpoint both declared and implemented has verification "
                        + "evidence from at least one configured contract verification source.");
                return;
            }

            Map<String, List<Endpoint>> byController = doppelgangers.stream()
                    .collect(Collectors.groupingBy(Endpoint::declaringClass, TreeMap::new, Collectors.toList()));

            byController.forEach((controller, controllerDoppelgangers) -> {
                writer.println("=== " + controller);
                writer.println();
                writer.println("[cols=\"1,3,3,2,1\",options=\"header\"]");
                writer.println("|===");
                writer.println("| HTTP Verb | Path | Method | Source File | Line");

                controllerDoppelgangers.stream()
                        .sorted(Comparator.comparing(Endpoint::path).thenComparing(e -> e.verb().name()))
                        .forEach(e -> {
                            writer.println();
                            writer.println("| " + e.verb());
                            writer.println("| " + e.path());
                            writer.println("| " + e.methodSignature());
                            writer.println("| " + e.sourceFile());
                            writer.println("| " + e.lineNumber());
                        });

                writer.println("|===");
                writer.println();
            });
        }
    }

    /**
     * Writes {@code warnings} as a single {@code [WARNING]} AsciiDoc admonition block, one bullet
     * per warning, or nothing at all when {@code warnings} is empty.
     */
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

    /**
     * Loads the "what is a doppelganger API" preamble bundled with the plugin as a classpath
     * resource.
     *
     * @return the preamble's AsciiDoc content
     * @throws IOException if the {@value #PREAMBLE_RESOURCE} resource is missing or cannot be read
     */
    private String loadPreamble() throws IOException {
        try (InputStream stream = DoppelgangerApiReportWriter.class.getClassLoader()
                .getResourceAsStream(PREAMBLE_RESOURCE)) {
            if (stream == null) {
                throw new IOException("Missing bundled resource: " + PREAMBLE_RESOURCE);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
