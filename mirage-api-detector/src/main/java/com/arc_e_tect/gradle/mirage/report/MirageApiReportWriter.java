package com.arc_e_tect.gradle.mirage.report;

import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;
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
 * Writes the AsciiDoc mirage API report: every OpenAPI operation that has no matching
 * {@code @RestController} implementation.
 *
 * <p>Every report opens with a preamble explaining what a mirage API is, loaded from the
 * {@value #PREAMBLE_RESOURCE} classpath resource so that explanatory text can be revised without
 * touching this class. When {@code scanMocks} was {@code true} for the run, a second preamble -
 * {@value #STUB_SCANNING_PREAMBLE_RESOURCE} - explaining what the stub scan can and cannot match,
 * shadow stubs, and multiple stub files covering one endpoint, is written right after it, since
 * every {@code Stubbed} figure the rest of the report shows depends on understanding those
 * limits. Whenever {@code contractHistory} is non-empty - i.e. whenever the
 * {@code == Progress Over Time} section itself is written - a third preamble,
 * {@value #REMOVED_PREAMBLE_RESOURCE}, explains why an endpoint becomes removed (postponed,
 * obsolete, or replaced after an error in its original definition), right before that section.</p>
 */
public class MirageApiReportWriter {

    /** Classpath resource holding the "what is a mirage API" preamble, bundled with the plugin. */
    static final String PREAMBLE_RESOURCE = "mirage-api-preamble.adoc";

    /**
     * Classpath resource holding the "about stub evidence" preamble, bundled with the plugin -
     * written only when {@code scanMocks} was {@code true} for the run.
     */
    static final String STUB_SCANNING_PREAMBLE_RESOURCE = "mirage-api-stub-scanning-preamble.adoc";

    /**
     * Classpath resource holding the "about removed endpoints" preamble, bundled with the plugin -
     * written only when {@code contractHistory} is non-empty, i.e. whenever the
     * {@code == Progress Over Time} section it explains is itself written.
     */
    static final String REMOVED_PREAMBLE_RESOURCE = "mirage-api-removed-preamble.adoc";

    /** Group heading used for mirage APIs whose OpenAPI operation declares no tags. */
    static final String UNTAGGED_GROUP = "(untagged)";

    private final ContractProgressTableWriter progressTableWriter = new ContractProgressTableWriter();

    /** Creates a new {@code MirageApiReportWriter}. */
    public MirageApiReportWriter() {}

    /**
     * Writes the report to {@code outputFile}, creating its parent directory if necessary.
     * Equivalent to calling {@link #write(File, int, List, String, List, Map)} with no warnings and
     * an empty contract history.
     *
     * @param outputFile             target AsciiDoc file
     * @param totalDescribedCount    total number of endpoints described by the OpenAPI documentation
     * @param mirages                the described endpoints not implemented by any controller
     * @param systemUnderTestVersion version of the system under test that was scanned
     * @throws IOException if the output file cannot be written
     */
    public void write(
            File outputFile, int totalDescribedCount, List<DescribedEndpoint> mirages,
            String systemUnderTestVersion) throws IOException {
        write(outputFile, totalDescribedCount, mirages, List.of(), systemUnderTestVersion, List.of(), Map.of());
    }

    /**
     * Writes the report to {@code outputFile}, creating its parent directory if necessary.
     * Equivalent to calling {@link #write(File, int, List, String, List, Map)} with no warnings.
     *
     * @param outputFile             target AsciiDoc file
     * @param totalDescribedCount    total number of endpoints described by the OpenAPI documentation
     * @param mirages                the described endpoints with no matching {@code @RestController}
     *                               implementation
     * @param systemUnderTestVersion version of the system under test that was scanned
     * @param contractHistory        contract progress history to render as a {@code == Progress Over
     *                                Time} section, keyed by fingerprint; when empty, no such
     *                                section is written
     * @throws IOException if the output file cannot be written
     */
    public void write(
            File outputFile, int totalDescribedCount, List<DescribedEndpoint> mirages,
            String systemUnderTestVersion, Map<String, ContractProgressRecord> contractHistory)
            throws IOException {
        write(outputFile, totalDescribedCount, mirages, List.of(), systemUnderTestVersion, List.of(), contractHistory);
    }

    /**
     * Writes the report to {@code outputFile}, creating its parent directory if necessary.
     * Equivalent to calling {@link #write(File, int, List, List, String, List, Map)} with an empty
     * excluded-mirages list.
     *
     * @param outputFile             target AsciiDoc file
     * @param totalDescribedCount    total number of endpoints described by the OpenAPI documentation
     * @param mirages                the described endpoints with no matching {@code @RestController}
     *                               implementation
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
    public void write(
            File outputFile, int totalDescribedCount, List<DescribedEndpoint> mirages,
            String systemUnderTestVersion, List<String> warnings,
            Map<String, ContractProgressRecord> contractHistory)
            throws IOException {
        write(outputFile, totalDescribedCount, mirages, List.of(), systemUnderTestVersion, warnings, contractHistory);
    }

    /**
     * Writes the report to {@code outputFile}, creating its parent directory if necessary.
     *
     * @param outputFile             target AsciiDoc file
     * @param totalDescribedCount    total number of endpoints described by the OpenAPI documentation
     * @param mirages                the described endpoints with no matching {@code @RestController}
     *                               implementation, excluding any matched by a configured exclusion
     *                               rule - these are the endpoints that can fail {@code failOnMirage}
     * @param excludedMirages        described-but-unimplemented endpoints matched by a configured
     *                                exclusion rule, paired with their current-run WireMock stub
     *                                status; rendered under {@code == Excluded Mirage APIs} instead
     *                                of {@code == Mirage APIs}, never fails the build, and never
     *                                appears in contract history - when empty, no such section is
     *                                written
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
    public void write(
            File outputFile, int totalDescribedCount, List<DescribedEndpoint> mirages,
            List<ExcludedMirage> excludedMirages, String systemUnderTestVersion, List<String> warnings,
            Map<String, ContractProgressRecord> contractHistory)
            throws IOException {
        write(outputFile, totalDescribedCount, mirages, excludedMirages, systemUnderTestVersion, warnings,
                contractHistory, false);
    }

    /**
     * Writes the report to {@code outputFile}, creating its parent directory if necessary.
     *
     * @param outputFile             target AsciiDoc file
     * @param totalDescribedCount    total number of endpoints described by the OpenAPI documentation
     * @param mirages                the described endpoints with no matching {@code @RestController}
     *                               implementation, excluding any matched by a configured exclusion
     *                               rule - these are the endpoints that can fail {@code failOnMirage}
     * @param excludedMirages        described-but-unimplemented endpoints matched by a configured
     *                                exclusion rule, paired with their current-run WireMock stub
     *                                status; rendered under {@code == Excluded Mirage APIs} instead
     *                                of {@code == Mirage APIs}, never fails the build, and never
     *                                appears in contract history - when empty, no such section is
     *                                written
     * @param systemUnderTestVersion version of the system under test that was scanned
     * @param warnings               non-fatal configuration gaps to render as a {@code WARNING}
     *                                admonition right after the report header - e.g. a configured
     *                                {@code rootDocument} or {@code controllerDirs} entry that
     *                                doesn't exist yet; when empty, no such admonition is written
     * @param contractHistory        contract progress history to render as a {@code == Progress Over
     *                                Time} section, keyed by fingerprint; when empty, no such
     *                                section is written
     * @param scanMocks              whether stub mapping/Java DSL scanning was enabled for this run -
     *                                when {@code true}, an additional preamble explaining what the
     *                                stub scan can and cannot match, shadow stubs, and multiple stub
     *                                files covering one endpoint is written right after the main
     *                                preamble
     * @throws IOException if the output file cannot be written
     */
    public void write(
            File outputFile, int totalDescribedCount, List<DescribedEndpoint> mirages,
            List<ExcludedMirage> excludedMirages, String systemUnderTestVersion, List<String> warnings,
            Map<String, ContractProgressRecord> contractHistory, boolean scanMocks)
            throws IOException {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create output directory: " + parent);
        }

        String missingEvidenceClause = "implemented by any `@RestController` class";
        String allEvidenceClause = "implemented by a scanned `@RestController` class";

        try (PrintWriter writer = new PrintWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.println("= Mirage API Report");
            writer.println(":toc:");
            writer.println(":toclevels: 2");
            writer.println();
            writer.println("System Under Test version: " + systemUnderTestVersion);
            writer.println();
            writer.println("Generated: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println();
            writeWarnings(writer, warnings);
            writer.println("Scanned " + totalDescribedCount
                    + " endpoint(s) described in the OpenAPI documentation. " + mirages.size()
                    + (mirages.size() == 1 ? " of them is not " : " of them are not ")
                    + missingEvidenceClause + ".");
            writer.println();
            writer.print(loadPreamble());
            writer.println();
            if (scanMocks) {
                writer.print(loadStubScanningPreamble());
                writer.println();
            }
            if (!contractHistory.isEmpty()) {
                writer.print(loadRemovedPreamble());
                writer.println();
            }
            progressTableWriter.write(writer, contractHistory);
            writer.println("== Mirage APIs");
            writer.println();

            if (mirages.isEmpty()) {
                writer.println("None found. Every endpoint described in the OpenAPI documentation is "
                        + allEvidenceClause + ".");
            } else {
                writeMirageTable(writer, mirages);
            }

            writeExcludedSection(writer, excludedMirages);
        }
    }

    private void writeMirageTable(PrintWriter writer, List<DescribedEndpoint> mirages) {
        Map<String, List<DescribedEndpoint>> byTag = mirages.stream()
                .collect(Collectors.groupingBy(this::primaryTag, TreeMap::new, Collectors.toList()));

        byTag.forEach((tag, tagMirages) -> {
            writer.println("=== " + tag);
            writer.println();
            writer.println("[cols=\"1,3,3,3\",options=\"header\"]");
            writer.println("|===");
            writer.println("| HTTP Verb | Path | Operation ID | Tags");

            tagMirages.stream()
                    .sorted(Comparator.comparing(DescribedEndpoint::path)
                            .thenComparing(e -> e.verb().name()))
                    .forEach(e -> {
                        List<String> tags = effectiveTags(e);
                        writer.println();
                        writer.println("| " + e.verb());
                        writer.println("| " + e.path());
                        writer.println("| " + (isBlank(e.operationId()) ? "(none)" : e.operationId()));
                        writer.println("| " + (tags.isEmpty() ? "(none)" : String.join(", ", tags)));
                    });

            writer.println("|===");
            writer.println();
        });
    }

    /**
     * Writes the {@code == Excluded Mirage APIs} section - endpoints that are, in fact, mirage
     * APIs (declared, unimplemented) but matched a configured exclusion rule, so they never fail
     * the build and are never written to contract history. Includes each one's current-run
     * WireMock stub status, since that's still operationally relevant to consumers even though the
     * endpoint is excluded from implementation-gap checking - see {@link StubStatus}. Written only
     * when {@code excludedMirages} is non-empty.
     */
    private void writeExcludedSection(PrintWriter writer, List<ExcludedMirage> excludedMirages) {
        if (excludedMirages.isEmpty()) {
            return;
        }
        writer.println();
        writer.println("== Excluded Mirage APIs");
        writer.println();
        writer.println("Declared in the OpenAPI documentation but not implemented by any `@RestController` "
                + "class - same as a mirage API - except matched by a configured exclusion rule, so it does "
                + "not fail the build and is not recorded in contract history.");
        writer.println();
        writer.println("[cols=\"1,3,3,1\",options=\"header\"]");
        writer.println("|===");
        writer.println("| HTTP Verb | Path | Operation ID | Stubbed");

        excludedMirages.stream()
                .sorted(Comparator.comparing((ExcludedMirage em) -> em.endpoint().path())
                        .thenComparing(em -> em.endpoint().verb().name()))
                .forEach(em -> {
                    DescribedEndpoint e = em.endpoint();
                    writer.println();
                    writer.println("| " + e.verb());
                    writer.println("| " + e.path());
                    writer.println("| " + (isBlank(e.operationId()) ? "(none)" : e.operationId()));
                    writer.println("| " + stubStatusLabel(em.stubStatus()));
                });

        writer.println("|===");
        writer.println();
    }

    private String stubStatusLabel(StubStatus status) {
        return switch (status) {
            case STUBBED -> "Yes";
            case NOT_STUBBED -> "No";
            case NOT_SCANNED -> "Not scanned";
        };
    }

    private String primaryTag(DescribedEndpoint endpoint) {
        List<String> tags = effectiveTags(endpoint);
        return tags.isEmpty() ? UNTAGGED_GROUP : tags.get(0);
    }

    /**
     * Returns {@code endpoint}'s tags with blank entries removed, so that an OpenAPI document
     * declaring e.g. {@code tags: [""]} - a real if uncommon generator quirk - is treated the
     * same as declaring no tags at all, rather than grouping under an empty heading.
     */
    private List<String> effectiveTags(DescribedEndpoint endpoint) {
        return endpoint.tags().stream().filter(tag -> !isBlank(tag)).toList();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
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
     * Loads the "what is a mirage API" preamble bundled with the plugin as a classpath resource.
     *
     * @return the preamble's AsciiDoc content
     * @throws IOException if the {@value #PREAMBLE_RESOURCE} resource is missing or cannot be read
     */
    private String loadPreamble() throws IOException {
        return loadResource(PREAMBLE_RESOURCE);
    }

    /**
     * Loads the "about stub evidence" preamble bundled with the plugin as a classpath resource -
     * see {@value #STUB_SCANNING_PREAMBLE_RESOURCE}.
     *
     * @return the preamble's AsciiDoc content
     * @throws IOException if the {@value #STUB_SCANNING_PREAMBLE_RESOURCE} resource is missing or
     *                      cannot be read
     */
    private String loadStubScanningPreamble() throws IOException {
        return loadResource(STUB_SCANNING_PREAMBLE_RESOURCE);
    }

    /**
     * Loads the "about removed endpoints" preamble bundled with the plugin as a classpath
     * resource - see {@value #REMOVED_PREAMBLE_RESOURCE}.
     *
     * @return the preamble's AsciiDoc content
     * @throws IOException if the {@value #REMOVED_PREAMBLE_RESOURCE} resource is missing or cannot
     *                      be read
     */
    private String loadRemovedPreamble() throws IOException {
        return loadResource(REMOVED_PREAMBLE_RESOURCE);
    }

    private String loadResource(String resourceName) throws IOException {
        try (InputStream stream = MirageApiReportWriter.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (stream == null) {
                throw new IOException("Missing bundled resource: " + resourceName);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
