package com.arc_e_tect.gradle.shadow.report;

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
 * Writes the AsciiDoc shadow API report: every {@code @RestController} endpoint that is not
 * described in the configured OpenAPI documentation.
 *
 * <p>Every report opens with a preamble explaining what a shadow API is, loaded from the
 * {@value #PREAMBLE_RESOURCE} classpath resource so that explanatory text can be revised without
 * touching this class.</p>
 */
public class ShadowApiReportWriter {

    /** Classpath resource holding the "what is a shadow API" preamble, bundled with the plugin. */
    static final String PREAMBLE_RESOURCE = "shadow-api-preamble.adoc";

    private final ContractProgressTableWriter progressTableWriter = new ContractProgressTableWriter();

    /** Creates a new {@code ShadowApiReportWriter}. */
    public ShadowApiReportWriter() {}

    /**
     * Writes the report to {@code outputFile}, creating its parent directory if necessary. Equivalent
     * to {@link #write(File, int, List, String, List, Map)} with no warnings and an empty contract
     * history.
     *
     * @param outputFile             target AsciiDoc file
     * @param totalEndpointCount     total number of endpoints found across all scanned controllers
     * @param shadows                the endpoints not described in the OpenAPI documentation
     * @param systemUnderTestVersion version of the system under test that was scanned
     * @throws IOException if the output file cannot be written
     */
    public void write(File outputFile, int totalEndpointCount, List<Endpoint> shadows, String systemUnderTestVersion)
            throws IOException {
        write(outputFile, totalEndpointCount, shadows, List.of(), systemUnderTestVersion, List.of(), Map.of());
    }

    /**
     * Writes the report to {@code outputFile}, creating its parent directory if necessary. Equivalent
     * to {@link #write(File, int, List, String, List, Map)} with no warnings.
     *
     * @param outputFile             target AsciiDoc file
     * @param totalEndpointCount     total number of endpoints found across all scanned controllers
     * @param shadows                the endpoints not described in the OpenAPI documentation
     * @param systemUnderTestVersion version of the system under test that was scanned
     * @param contractHistory        contract progress history to render as a {@code == Progress Over
     *                                Time} section, keyed by fingerprint; when empty, no such
     *                                section is written
     * @throws IOException if the output file cannot be written
     */
    public void write(File outputFile, int totalEndpointCount, List<Endpoint> shadows, String systemUnderTestVersion,
            Map<String, ContractProgressRecord> contractHistory) throws IOException {
        write(outputFile, totalEndpointCount, shadows, List.of(), systemUnderTestVersion, List.of(), contractHistory);
    }

    /**
     * Writes the report to {@code outputFile}, creating its parent directory if necessary.
     * Equivalent to calling {@link #write(File, int, List, List, String, List, Map)} with an empty
     * excluded-implementations list.
     *
     * @param outputFile             target AsciiDoc file
     * @param totalEndpointCount     total number of endpoints found across all scanned controllers
     * @param shadows                the endpoints not described in the OpenAPI documentation
     * @param systemUnderTestVersion version of the system under test that was scanned
     * @param warnings               non-fatal configuration gaps to render as a {@code WARNING}
     *                                admonition right after the report header - e.g. a configured
     *                                {@code rootDocument} or {@code controllerDirs} entry that
     *                                doesn't exist yet; when empty, no such admonition is written
     * @param contractHistory        contract progress history to render as a {@code == Progress Over
     *                                Time} section, keyed by fingerprint; when empty, no such
     *                                section is written
     * @throws IOException if the output file cannot be written
     */
    public void write(File outputFile, int totalEndpointCount, List<Endpoint> shadows, String systemUnderTestVersion,
            List<String> warnings, Map<String, ContractProgressRecord> contractHistory) throws IOException {
        write(outputFile, totalEndpointCount, shadows, List.of(), systemUnderTestVersion, warnings, contractHistory);
    }

    /**
     * Writes the report to {@code outputFile}, creating its parent directory if necessary.
     *
     * @param outputFile               target AsciiDoc file
     * @param totalEndpointCount       total number of endpoints found across all scanned controllers
     * @param shadows                  the endpoints not described in the OpenAPI documentation,
     *                                 excluding any matched by a configured exclusion rule - these
     *                                 are the endpoints that can fail {@code failOnShadow}
     * @param excludedImplementations every scanned endpoint matched by a configured exclusion rule
     *                                 (whether or not it is also undocumented) - a real
     *                                 implementation exists at a path declared excluded, which
     *                                 undermines the assumption behind the exclusion; rendered
     *                                 under {@code == Excluded Implementations} instead of
     *                                 {@code == Shadow APIs}, never fails the build, and never
     *                                 appears in contract history - when empty, no such section is
     *                                 written
     * @param systemUnderTestVersion   version of the system under test that was scanned
     * @param warnings                 non-fatal configuration gaps to render as a {@code WARNING}
     *                                  admonition right after the report header - e.g. a configured
     *                                  {@code rootDocument} or {@code controllerDirs} entry that
     *                                  doesn't exist yet; when empty, no such admonition is written
     * @param contractHistory          contract progress history to render as a {@code == Progress
     *                                  Over Time} section, keyed by fingerprint; when empty, no such
     *                                  section is written
     * @throws IOException if the output file cannot be written
     */
    public void write(File outputFile, int totalEndpointCount, List<Endpoint> shadows,
            List<Endpoint> excludedImplementations, String systemUnderTestVersion,
            List<String> warnings, Map<String, ContractProgressRecord> contractHistory) throws IOException {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create output directory: " + parent);
        }

        try (PrintWriter writer = new PrintWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.println("= Shadow API Report");
            writer.println(":toc:");
            writer.println(":toclevels: 2");
            writer.println();
            writer.println("System Under Test version: " + systemUnderTestVersion);
            writer.println();
            writer.println("Generated: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println();
            writeWarnings(writer, warnings);
            writer.println("Scanned " + totalEndpointCount
                    + " endpoint(s) exposed by `@RestController` classes. " + shadows.size()
                    + (shadows.size() == 1 ? " of them is" : " of them are")
                    + " not described in the OpenAPI documentation.");
            writer.println();
            writer.print(loadPreamble());
            writer.println();
            progressTableWriter.write(writer, contractHistory);
            writer.println("== Shadow APIs");
            writer.println();

            if (shadows.isEmpty()) {
                writer.println("None found. Every endpoint exposed by the scanned controllers is described "
                        + "in the OpenAPI documentation.");
            } else {
                writeEndpointTable(writer, shadows);
            }

            writeExcludedSection(writer, excludedImplementations);
        }
    }

    private void writeEndpointTable(PrintWriter writer, List<Endpoint> endpoints) {
        Map<String, List<Endpoint>> byController = endpoints.stream()
                .collect(Collectors.groupingBy(Endpoint::declaringClass, TreeMap::new, Collectors.toList()));

        byController.forEach((controller, controllerEndpoints) -> {
            writer.println("=== " + controller);
            writer.println();
            writer.println("[cols=\"1,3,3,2,1\",options=\"header\"]");
            writer.println("|===");
            writer.println("| HTTP Verb | Path | Method | Source File | Line");

            controllerEndpoints.stream()
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

    /**
     * Writes the {@code == Excluded Implementations} section - real {@code @RestController}
     * implementations found at a path a configured exclusion rule declares excluded, regardless of
     * whether that endpoint is also documented. Worth surfacing on its own: an exclusion rule
     * exists because no real implementation was expected at that path (e.g. it's meant to be
     * framework-provided), so an actual implementation there undermines that assumption. Never
     * fails the build and never reaches contract history. Written only when
     * {@code excludedImplementations} is non-empty.
     */
    private void writeExcludedSection(PrintWriter writer, List<Endpoint> excludedImplementations) {
        if (excludedImplementations.isEmpty()) {
            return;
        }
        writer.println();
        writer.println("== Excluded Implementations");
        writer.println();
        writer.println("A real `@RestController` implementation exists at a path a configured exclusion "
                + "rule declares excluded - regardless of whether it is also documented. Worth reviewing: "
                + "the exclusion exists because no real implementation was expected here.");
        writer.println();
        writeEndpointTable(writer, excludedImplementations);
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
     * Loads the "what is a shadow API" preamble bundled with the plugin as a classpath resource.
     *
     * @return the preamble's AsciiDoc content
     * @throws IOException if the {@value #PREAMBLE_RESOURCE} resource is missing or cannot be read
     */
    private String loadPreamble() throws IOException {
        try (InputStream stream = ShadowApiReportWriter.class.getClassLoader().getResourceAsStream(PREAMBLE_RESOURCE)) {
            if (stream == null) {
                throw new IOException("Missing bundled resource: " + PREAMBLE_RESOURCE);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
