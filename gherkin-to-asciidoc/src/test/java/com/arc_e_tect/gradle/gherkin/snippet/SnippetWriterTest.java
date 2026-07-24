package com.arc_e_tect.gradle.gherkin.snippet;

import com.arc_e_tect.gradle.gherkin.parser.ScenarioInfo;
import com.arc_e_tect.gradle.gherkin.progress.ScenarioStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SnippetWriter")
class SnippetWriterTest {

    private final SnippetWriter writer = new SnippetWriter();

    @Test
    @DisplayName("writes one flat snippet file when groupByFeature is false")
    void writesFlatSnippetFileWhenNotGrouped(@TempDir Path tempDir) throws IOException {
        ScenarioInfo auth = new ScenarioInfo("Authentication", "Scenario: A", List.of());
        ScenarioInfo billing = new ScenarioInfo("Billing", "Scenario: B", List.of());

        StatusSnippets result = writer.writeStatus(
                tempDir.toFile(), ScenarioStatus.LISTED, List.of(auth, billing), false);

        assertThat(result.features()).isEmpty();
        assertThat(result.flatFile()).isEqualTo(new File(tempDir.toFile(), "listed.adoc"));
        String content = Files.readString(result.flatFile().toPath(), StandardCharsets.UTF_8);
        assertThat(content).contains("* Scenario: A", "* Scenario: B");
    }

    @Test
    @DisplayName("writes a None flat snippet file for an empty status")
    void writesNoneFlatSnippetFileForEmptyStatus(@TempDir Path tempDir) throws IOException {
        StatusSnippets result = writer.writeStatus(tempDir.toFile(), ScenarioStatus.DEFINED, List.of(), true);

        assertThat(result.features()).isEmpty();
        assertThat(result.flatFile()).isEqualTo(new File(tempDir.toFile(), "defined.adoc"));
        String content = Files.readString(result.flatFile().toPath(), StandardCharsets.UTF_8);
        assertThat(content.trim()).isEqualTo("_None._");
    }

    @Test
    @DisplayName("writes one snippet file per feature, in a camelCase directory, when grouped")
    void writesOneSnippetFilePerFeatureWhenGrouped(@TempDir Path tempDir) throws IOException {
        ScenarioInfo auth = new ScenarioInfo("User authentication", "Scenario: A", List.of());
        ScenarioInfo billing = new ScenarioInfo("Invoice payment", "Scenario: B", List.of());

        StatusSnippets result = writer.writeStatus(
                tempDir.toFile(), ScenarioStatus.IMPLEMENTED, List.of(auth, billing), true);

        assertThat(result.flatFile()).isNull();
        assertThat(result.features()).extracting(FeatureSnippet::featureTitle)
                .containsExactly("User authentication", "Invoice payment");

        File authFile = new File(new File(tempDir.toFile(), "userAuthentication"), "implemented.adoc");
        File billingFile = new File(new File(tempDir.toFile(), "invoicePayment"), "implemented.adoc");
        assertThat(result.features().get(0).file()).isEqualTo(authFile);
        assertThat(result.features().get(1).file()).isEqualTo(billingFile);
        assertThat(Files.readString(authFile.toPath(), StandardCharsets.UTF_8)).contains("* Scenario: A");
        assertThat(Files.readString(billingFile.toPath(), StandardCharsets.UTF_8)).contains("* Scenario: B");
    }

    @Test
    @DisplayName("throws a GradleException when the snippet file cannot be written")
    void throwsWhenSnippetFileCannotBeWritten(@TempDir Path tempDir) throws IOException {
        // Pre-create "listed.adoc" as a directory, so writing the snippet file to that same path fails.
        Files.createDirectory(tempDir.resolve("listed.adoc"));

        assertThatThrownBy(() -> writer.writeStatus(tempDir.toFile(), ScenarioStatus.LISTED, List.of(
                new ScenarioInfo("Feature", "Scenario: A", List.of())), false))
                .isInstanceOf(org.gradle.api.GradleException.class);
    }
}
