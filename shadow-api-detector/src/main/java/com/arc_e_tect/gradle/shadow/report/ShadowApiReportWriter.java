package com.arc_e_tect.gradle.shadow.report;

import com.arc_e_tect.gradle.shadow.model.Endpoint;

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

    /** Creates a new {@code ShadowApiReportWriter}. */
    public ShadowApiReportWriter() {}

    /**
     * Writes the report to {@code outputFile}, creating its parent directory if necessary.
     *
     * @param outputFile             target AsciiDoc file
     * @param totalEndpointCount     total number of endpoints found across all scanned controllers
     * @param shadows                the endpoints not described in the OpenAPI documentation
     * @param systemUnderTestVersion version of the system under test that was scanned
     * @throws IOException if the output file cannot be written
     */
    public void write(File outputFile, int totalEndpointCount, List<Endpoint> shadows, String systemUnderTestVersion)
            throws IOException {
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
            writer.println("Scanned " + totalEndpointCount
                    + " endpoint(s) exposed by `@RestController` classes. " + shadows.size()
                    + (shadows.size() == 1 ? " of them is" : " of them are")
                    + " not described in the OpenAPI documentation.");
            writer.println();
            writer.print(loadPreamble());
            writer.println();
            writer.println("== Shadow APIs");
            writer.println();

            if (shadows.isEmpty()) {
                writer.println("None found. Every endpoint exposed by the scanned controllers is described "
                        + "in the OpenAPI documentation.");
                return;
            }

            Map<String, List<Endpoint>> byController = shadows.stream()
                    .collect(Collectors.groupingBy(Endpoint::declaringClass, TreeMap::new, Collectors.toList()));

            byController.forEach((controller, controllerShadows) -> {
                writer.println("=== " + controller);
                writer.println();
                writer.println("[cols=\"1,3,3,2,1\",options=\"header\"]");
                writer.println("|===");
                writer.println("| HTTP Verb | Path | Method | Source File | Line");

                controllerShadows.stream()
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
