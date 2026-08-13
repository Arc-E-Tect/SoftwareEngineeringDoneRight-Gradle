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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ListTrackerLensStylesTask")
class ListTrackerLensStylesTaskTest {

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
    @DisplayName("listShouldPrintTheThreeBuiltInLensesGroupedUnderBuiltIn")
    void listShouldPrintTheThreeBuiltInLensesGroupedUnderBuiltIn() {
        ListTrackerLensStylesTask task = newTask();

        task.list();

        String output = capturedOut.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("Built-in:", "light-lens", "dark-lens", "high-contrast-lens");
    }

    @Test
    @DisplayName("listShouldPrintExternalPackLensesGroupedUnderTheirDerivedPackLabel")
    void listShouldPrintExternalPackLensesGroupedUnderTheirDerivedPackLabel() throws IOException {
        ListTrackerLensStylesTask task = newTask();
        File jarFile = writeStylePackJar("midnight-theme.jar", "midnight");
        task.getLensStyleClasspath().from(jarFile);

        task.list();

        String output = capturedOut.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("midnight-theme:", "midnight");
    }

    @Test
    @DisplayName("listShouldPrintCustomLensWhenLensStylesheetConfigured")
    void listShouldPrintCustomLensWhenLensStylesheetConfigured() throws IOException {
        ListTrackerLensStylesTask task = newTask();
        Path stylesheet = tempDir.resolve("my-theme.css");
        Files.writeString(stylesheet, "body {}");
        task.getLensStylesheet().set(stylesheet.toFile());

        task.list();

        String output = capturedOut.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("Custom (lensStylesheet):", "custom-lens");
    }

    @Test
    @DisplayName("listShouldPrintTotalDiscoveredCountInTheHeader")
    void listShouldPrintTotalDiscoveredCountInTheHeader() {
        ListTrackerLensStylesTask task = newTask();

        task.list();

        String output = capturedOut.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("Tracker Lens styles available (3):");
    }

    private ListTrackerLensStylesTask newTask() {
        return ProjectBuilder.builder()
                .withProjectDir(tempDir.toFile())
                .build()
                .getTasks()
                .create("listTrackerLensStyles", ListTrackerLensStylesTask.class);
    }

    private File writeStylePackJar(String fileName, String lensBaseName) throws IOException {
        Path jarFile = tempDir.resolve(fileName);
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(jarFile.toFile()))) {
            jar.putNextEntry(new JarEntry("META-INF/arc-e-tect/tracker-lens/lenses/" + lensBaseName + ".css"));
            jar.write(".dashboard {}".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        return jarFile.toFile();
    }
}
