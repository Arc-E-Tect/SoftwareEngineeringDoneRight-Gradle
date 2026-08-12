package com.arc_e_tect.gradle.doppelganger.report;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;

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

    /** Creates a new {@code DoppelgangerApiReportWriter}. */
    public DoppelgangerApiReportWriter() {}

    /**
     * Writes the report to {@code outputFile}, creating its parent directory if necessary.
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
            writer.println("Scanned " + totalCandidateCount
                    + " endpoint(s) both declared in the OpenAPI documentation and implemented by a "
                    + "`@RestController` class. " + doppelgangers.size()
                    + (doppelgangers.size() == 1 ? " of them is" : " of them are")
                    + " not verified by any configured contract verification source.");
            writer.println();
            writer.print(loadPreamble());
            writer.println();
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
