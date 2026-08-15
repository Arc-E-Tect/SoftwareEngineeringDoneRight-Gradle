package com.arc_e_tect.gradle.trackerlens.lens;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TemplateScanner")
class TemplateScannerTest {

    @TempDir
    Path tempDir;

    private final TemplateScanner scanner = new TemplateScanner();

    @Test
    @DisplayName("scanShouldDiscoverTemplatesFromDirectory")
    void scanShouldDiscoverTemplatesFromDirectory() throws IOException {
        Path templateDir = tempDir.resolve("META-INF/arc-e-tect/tracker-lens/templates");
        Files.createDirectories(templateDir);
        Files.writeString(templateDir.resolve("Venn-Diagram-View.html"), "<html></html>");

        List<Template> templates = scanner.scan(tempDir.toFile());

        assertThat(templates).extracting(Template::id).containsExactly("venn-diagram-view");
    }

    @Test
    @DisplayName("scanShouldDiscoverTemplatesFromJar")
    void scanShouldDiscoverTemplatesFromJar() throws IOException {
        Path jarFile = tempDir.resolve("view-pack.jar");
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(jarFile.toFile()))) {
            jar.putNextEntry(new JarEntry("META-INF/arc-e-tect/tracker-lens/templates/dashboard-story.html"));
            jar.write("<html></html>".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("META-INF/arc-e-tect/tracker-lens/templates/other/nested.html"));
            jar.write("ignored".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }

        List<Template> templates = scanner.scan(jarFile.toFile());

        assertThat(templates).extracting(Template::id).containsExactly("dashboard-story");
    }

    @Test
    @DisplayName("scanShouldReturnEmptyWhenDirectoryHasNoTemplatesFolder")
    void scanShouldReturnEmptyWhenDirectoryHasNoTemplatesFolder() {
        List<Template> templates = scanner.scan(tempDir.toFile());

        assertThat(templates).isEmpty();
    }

    @Test
    @DisplayName("scanShouldFailWhenJarShipsTwoTemplateFilesResolvingToSameId")
    void scanShouldFailWhenJarShipsTwoTemplateFilesResolvingToSameId() throws IOException {
        Path jarFile = tempDir.resolve("colliding-pack.jar");
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(jarFile.toFile()))) {
            jar.putNextEntry(new JarEntry("META-INF/arc-e-tect/tracker-lens/templates/Story-View.html"));
            jar.write("<html>a</html>".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("META-INF/arc-e-tect/tracker-lens/templates/story-view.html"));
            jar.write("<html>b</html>".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }

        assertThatThrownBy(() -> scanner.scan(jarFile.toFile()))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("story-view");
    }
}
