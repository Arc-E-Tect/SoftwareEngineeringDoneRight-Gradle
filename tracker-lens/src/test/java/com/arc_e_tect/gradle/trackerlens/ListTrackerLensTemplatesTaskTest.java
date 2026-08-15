package com.arc_e_tect.gradle.trackerlens;

import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ListTrackerLensTemplatesTask")
class ListTrackerLensTemplatesTaskTest {

    @TempDir
    Path tempDir;

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream capturedOut;

    @BeforeEach
    void captureStdOut() {
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreStdOut() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("listShouldPrintTheBuiltInTemplateGroupedUnderBuiltIn")
    void listShouldPrintTheBuiltInTemplateGroupedUnderBuiltIn() {
        ListTrackerLensTemplatesTask task = newTask();

        task.list();

        String output = capturedOut.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("Built-in:", "default");
    }

    @Test
    @DisplayName("listShouldPrintExternalPackTemplatesGroupedUnderTheirDerivedPackLabel")
    void listShouldPrintExternalPackTemplatesGroupedUnderTheirDerivedPackLabel() throws IOException {
        ListTrackerLensTemplatesTask task = newTask();
        File jarFile = writeTemplatePackJar("venn-view-pack.jar", "venn-diagram-view");
        task.getLensStyleClasspath().from(jarFile);

        task.list();

        String output = capturedOut.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("venn-view-pack:", "venn-diagram-view");
    }

    @Test
    @DisplayName("listShouldPrintTotalDiscoveredCountInTheHeader")
    void listShouldPrintTotalDiscoveredCountInTheHeader() {
        ListTrackerLensTemplatesTask task = newTask();

        task.list();

        String output = capturedOut.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("Tracker Lens templates available (1):");
    }

    private ListTrackerLensTemplatesTask newTask() {
        return ProjectBuilder.builder()
                .withProjectDir(tempDir.toFile())
                .build()
                .getTasks()
                .create("listTrackerLensTemplates", ListTrackerLensTemplatesTask.class);
    }

    private File writeTemplatePackJar(String fileName, String templateBaseName) throws IOException {
        Path jarFile = tempDir.resolve(fileName);
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(jarFile.toFile()))) {
            jar.putNextEntry(new JarEntry("META-INF/arc-e-tect/tracker-lens/templates/" + templateBaseName + ".html"));
            jar.write("<html></html>".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        return jarFile.toFile();
    }
}
