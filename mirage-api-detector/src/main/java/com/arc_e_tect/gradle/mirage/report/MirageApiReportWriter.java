package com.arc_e_tect.gradle.mirage.report;

import com.arc_e_tect.gradle.mirage.openapi.DescribedEndpoint;

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
 * touching this class.</p>
 */
public class MirageApiReportWriter {

    /** Classpath resource holding the "what is a mirage API" preamble, bundled with the plugin. */
    static final String PREAMBLE_RESOURCE = "mirage-api-preamble.adoc";

    /** Group heading used for mirage APIs whose OpenAPI operation declares no tags. */
    static final String UNTAGGED_GROUP = "(untagged)";

    /** Creates a new {@code MirageApiReportWriter}. */
    public MirageApiReportWriter() {}

    /**
     * Writes the report to {@code outputFile}, creating its parent directory if necessary.
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
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create output directory: " + parent);
        }

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
            writer.println("Scanned " + totalDescribedCount
                    + " endpoint(s) described in the OpenAPI documentation. " + mirages.size()
                    + (mirages.size() == 1 ? " of them is" : " of them are")
                    + " not implemented by any `@RestController` class.");
            writer.println();
            writer.print(loadPreamble());
            writer.println();
            writer.println("== Mirage APIs");
            writer.println();

            if (mirages.isEmpty()) {
                writer.println("None found. Every endpoint described in the OpenAPI documentation is implemented "
                        + "by a scanned `@RestController` class.");
                return;
            }

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
                            writer.println();
                            writer.println("| " + e.verb());
                            writer.println("| " + e.path());
                            writer.println("| " + (e.operationId() == null ? "(none)" : e.operationId()));
                            writer.println("| " + (e.tags().isEmpty() ? "(none)" : String.join(", ", e.tags())));
                        });

                writer.println("|===");
                writer.println();
            });
        }
    }

    private String primaryTag(DescribedEndpoint endpoint) {
        return endpoint.tags().isEmpty() ? UNTAGGED_GROUP : endpoint.tags().get(0);
    }

    /**
     * Loads the "what is a mirage API" preamble bundled with the plugin as a classpath resource.
     *
     * @return the preamble's AsciiDoc content
     * @throws IOException if the {@value #PREAMBLE_RESOURCE} resource is missing or cannot be read
     */
    private String loadPreamble() throws IOException {
        try (InputStream stream = MirageApiReportWriter.class.getClassLoader().getResourceAsStream(PREAMBLE_RESOURCE)) {
            if (stream == null) {
                throw new IOException("Missing bundled resource: " + PREAMBLE_RESOURCE);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
