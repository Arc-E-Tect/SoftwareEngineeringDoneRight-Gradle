package com.arc_e_tect.gradle.zombie.report;

import com.arc_e_tect.gradle.zombie.model.Endpoint;

import java.io.File;
import java.io.IOException;
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
 * Writes the AsciiDoc zombie API report: every {@code @RestController} endpoint that is not
 * described in the configured OpenAPI documentation.
 */
public class ZombieApiReportWriter {

    /** Creates a new {@code ZombieApiReportWriter}. */
    public ZombieApiReportWriter() {}

    /**
     * Writes the report to {@code outputFile}, creating its parent directory if necessary.
     *
     * @param outputFile         target AsciiDoc file
     * @param totalEndpointCount total number of endpoints found across all scanned controllers
     * @param zombies            the endpoints not described in the OpenAPI documentation
     * @throws IOException if the output file cannot be written
     */
    public void write(File outputFile, int totalEndpointCount, List<Endpoint> zombies) throws IOException {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create output directory: " + parent);
        }

        try (PrintWriter writer = new PrintWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.println("= Zombie API Report");
            writer.println(":toc:");
            writer.println(":toclevels: 2");
            writer.println();
            writer.println("Generated: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println();
            writer.println("Scanned " + totalEndpointCount
                    + " endpoint(s) exposed by `@RestController` classes. " + zombies.size()
                    + (zombies.size() == 1 ? " of them is" : " of them are")
                    + " not described in the OpenAPI documentation.");
            writer.println();
            writer.println("== Zombie APIs");
            writer.println();

            if (zombies.isEmpty()) {
                writer.println("None found. Every endpoint exposed by the scanned controllers is described "
                        + "in the OpenAPI documentation.");
                return;
            }

            Map<String, List<Endpoint>> byController = zombies.stream()
                    .collect(Collectors.groupingBy(Endpoint::declaringClass, TreeMap::new, Collectors.toList()));

            byController.forEach((controller, controllerZombies) -> {
                writer.println("=== " + controller);
                writer.println();
                writer.println("[cols=\"1,3,3,2,1\",options=\"header\"]");
                writer.println("|===");
                writer.println("| HTTP Verb | Path | Method | Source File | Line");

                controllerZombies.stream()
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
}
