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

@DisplayName("LensScanner")
class LensScannerTest {

    @TempDir
    Path tempDir;

    private final LensScanner scanner = new LensScanner();

    @Test
    @DisplayName("scanShouldDiscoverLensesFromDirectory")
    void scanShouldDiscoverLensesFromDirectory() throws IOException {
        Path lensDir = tempDir.resolve("META-INF/arc-e-tect/tracker-lens/lenses");
        Files.createDirectories(lensDir);
        Files.writeString(lensDir.resolve("Dark-Lens.css"), ".dashboard { color: white; }");

        List<Lens> lenses = scanner.scan(tempDir.toFile());

        assertThat(lenses).extracting(Lens::id).containsExactly("dark-lens");
    }

    @Test
    @DisplayName("scanShouldDiscoverLensesFromJar")
    void scanShouldDiscoverLensesFromJar() throws IOException {
        Path jarFile = tempDir.resolve("style-pack.jar");
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(jarFile.toFile()))) {
            jar.putNextEntry(new JarEntry("META-INF/arc-e-tect/tracker-lens/lenses/midnight-theme.css"));
            jar.write(".dashboard { color: navy; }".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("META-INF/arc-e-tect/tracker-lens/lenses/other/nested.css"));
            jar.write("ignored".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }

        List<Lens> lenses = scanner.scan(jarFile.toFile());

        assertThat(lenses).extracting(Lens::id).containsExactly("midnight-theme");
    }

    @Test
    @DisplayName("scanShouldReturnEmptyWhenDirectoryHasNoLensesFolder")
    void scanShouldReturnEmptyWhenDirectoryHasNoLensesFolder() {
        List<Lens> lenses = scanner.scan(tempDir.toFile());

        assertThat(lenses).isEmpty();
    }

    @Test
    @DisplayName("scanShouldFailWhenJarShipsTwoLensFilesResolvingToSameId")
    void scanShouldFailWhenJarShipsTwoLensFilesResolvingToSameId() throws IOException {
        Path jarFile = tempDir.resolve("colliding-pack.jar");
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(jarFile.toFile()))) {
            jar.putNextEntry(new JarEntry("META-INF/arc-e-tect/tracker-lens/lenses/Dark-Lens.css"));
            jar.write("a {}".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("META-INF/arc-e-tect/tracker-lens/lenses/dark-lens.css"));
            jar.write("b {}".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }

        assertThatThrownBy(() -> scanner.scan(jarFile.toFile()))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("dark-lens");
    }
}
