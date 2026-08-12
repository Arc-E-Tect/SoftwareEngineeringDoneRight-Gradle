package com.arc_e_tect.gradle.doppelganger.report;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DoppelgangerApiReportWriter")
class DoppelgangerApiReportWriterTest {

    private final DoppelgangerApiReportWriter writer = new DoppelgangerApiReportWriter();

    @Test
    @DisplayName("reports that no doppelgangers were found when the list is empty")
    void reportsNoDoppelgangersFound(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");

        writer.write(output, 3, List.of(), "1.0.0");

        String content = Files.readString(output.toPath());
        assertThat(content)
                .contains("= Doppelganger API Report")
                .contains("Scanned 3 endpoint(s)")
                .contains("None found. Every endpoint both declared and implemented has verification");
    }

    @Test
    @DisplayName("lists every doppelganger grouped by declaring controller")
    void listsDoppelgangersGroupedByController(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");
        List<Endpoint> doppelgangers = List.of(
                new Endpoint(HttpVerb.POST, "/api/orders/{id}", "com.example.OrderController",
                        "createOrder(Long)", "OrderController.java", 42),
                new Endpoint(HttpVerb.GET, "/api/invoices", "com.example.InvoiceController",
                        "listInvoices()", "InvoiceController.java", 10));

        writer.write(output, 5, doppelgangers, "1.0.0");

        String content = Files.readString(output.toPath());
        assertThat(content)
                .contains("Scanned 5 endpoint(s)")
                .contains("2 of them are not verified")
                .contains("=== com.example.OrderController")
                .contains("=== com.example.InvoiceController")
                .contains("/api/orders/{id}")
                .contains("createOrder(Long)")
                .contains("OrderController.java")
                .contains("/api/invoices")
                .contains("listInvoices()");
    }

    @Test
    @DisplayName("creates the parent directory when it does not yet exist")
    void createsParentDirectory(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "nested/dir/report.adoc");

        writer.write(output, 0, List.of(), "1.0.0");

        assertThat(output).exists();
    }

    @Test
    @DisplayName("uses singular phrasing for exactly one doppelganger")
    void usesSingularPhrasingForOneDoppelganger(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");
        List<Endpoint> doppelgangers = List.of(
                new Endpoint(HttpVerb.GET, "/api/invoices", "com.example.InvoiceController",
                        "listInvoices()", "InvoiceController.java", 10));

        writer.write(output, 1, doppelgangers, "1.0.0");

        assertThat(Files.readString(output.toPath())).contains("1 of them is not verified");
    }

    @Test
    @DisplayName("includes the given system under test version")
    void includesSystemUnderTestVersion(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");

        writer.write(output, 0, List.of(), "v2.3.1");

        assertThat(Files.readString(output.toPath())).contains("System Under Test version: v2.3.1");
    }

    @Test
    @DisplayName("includes the bundled preamble explaining what a doppelganger API is")
    void includesBundledPreamble(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");

        writer.write(output, 0, List.of(), "1.0.0");

        String content = Files.readString(output.toPath());
        assertThat(content)
                .contains("What Is a Doppelganger API?")
                .contains("Shadow API")
                .contains("owasp.org/API-Security");
    }

    @Test
    @DisplayName("preamble content matches the bundled doppelganger-api-preamble.adoc resource verbatim")
    void preambleMatchesBundledResource(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");

        writer.write(output, 0, List.of(), "1.0.0");

        String content = Files.readString(output.toPath());
        String preamble;
        try (var stream = getClass().getClassLoader()
                .getResourceAsStream(DoppelgangerApiReportWriter.PREAMBLE_RESOURCE)) {
            preamble = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }

        assertThat(content).contains(preamble);
    }
}
