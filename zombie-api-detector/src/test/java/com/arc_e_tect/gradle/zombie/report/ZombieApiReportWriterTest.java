package com.arc_e_tect.gradle.zombie.report;

import com.arc_e_tect.gradle.zombie.model.Endpoint;
import com.arc_e_tect.gradle.zombie.model.HttpVerb;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ZombieApiReportWriter")
class ZombieApiReportWriterTest {

    private final ZombieApiReportWriter writer = new ZombieApiReportWriter();

    @Test
    @DisplayName("reports that no zombies were found when the list is empty")
    void reportsNoZombiesFound(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");

        writer.write(output, 3, List.of());

        String content = Files.readString(output.toPath());
        assertThat(content)
                .contains("= Zombie API Report")
                .contains("Scanned 3 endpoint(s)")
                .contains("None found. Every endpoint exposed by the scanned controllers is described");
    }

    @Test
    @DisplayName("lists every zombie grouped by declaring controller")
    void listsZombiesGroupedByController(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");
        List<Endpoint> zombies = List.of(
                new Endpoint(HttpVerb.POST, "/api/users/{id}", "com.example.UserController",
                        "createUser(Long)", "UserController.java", 42),
                new Endpoint(HttpVerb.GET, "/api/orders", "com.example.OrderController",
                        "listOrders()", "OrderController.java", 10));

        writer.write(output, 5, zombies);

        String content = Files.readString(output.toPath());
        assertThat(content)
                .contains("Scanned 5 endpoint(s)")
                .contains("2 of them are not described")
                .contains("=== com.example.UserController")
                .contains("=== com.example.OrderController")
                .contains("/api/users/{id}")
                .contains("createUser(Long)")
                .contains("UserController.java")
                .contains("/api/orders")
                .contains("listOrders()");
    }

    @Test
    @DisplayName("creates the parent directory when it does not yet exist")
    void createsParentDirectory(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "nested/dir/report.adoc");

        writer.write(output, 0, List.of());

        assertThat(output).exists();
    }

    @Test
    @DisplayName("uses singular phrasing for exactly one zombie")
    void usesSingularPhrasingForOneZombie(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");
        List<Endpoint> zombies = List.of(
                new Endpoint(HttpVerb.GET, "/api/orders", "com.example.OrderController",
                        "listOrders()", "OrderController.java", 10));

        writer.write(output, 1, zombies);

        assertThat(Files.readString(output.toPath())).contains("1 of them is not described");
    }
}
