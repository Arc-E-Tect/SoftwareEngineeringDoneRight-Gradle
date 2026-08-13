package com.arc_e_tect.gradle.trackerlens.lens;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
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

@DisplayName("LensSetResolver")
class LensSetResolverTest {

    @TempDir
    Path tempDir;

    private final LensSetResolver resolver = new LensSetResolver();

    @Test
    @DisplayName("resolveShouldAlwaysIncludeTheThreeBuiltInLenses")
    void resolveShouldAlwaysIncludeTheThreeBuiltInLenses() {
        List<ResolvedLens> lenses = resolver.resolve(null, List.of(), "");

        assertThat(lenses).extracting(ResolvedLens::id)
                .containsExactlyInAnyOrder("light-lens", "dark-lens", "high-contrast-lens");
    }

    @Test
    @DisplayName("resolveShouldIncludeExternalPackLensesFromClasspathEntries")
    void resolveShouldIncludeExternalPackLensesFromClasspathEntries() throws IOException {
        File jarFile = writeStylePackJar("midnight-theme.jar", "midnight");

        List<ResolvedLens> lenses = resolver.resolve(null, List.of(jarFile), "");

        assertThat(lenses).extracting(ResolvedLens::id).contains("midnight");
    }

    @Test
    @DisplayName("resolveShouldSkipClasspathEntriesThatDoNotExist")
    void resolveShouldSkipClasspathEntriesThatDoNotExist() {
        File missing = tempDir.resolve("does-not-exist.jar").toFile();

        List<ResolvedLens> lenses = resolver.resolve(null, List.of(missing), "");

        assertThat(lenses).extracting(ResolvedLens::id)
                .containsExactlyInAnyOrder("light-lens", "dark-lens", "high-contrast-lens");
    }

    @Test
    @DisplayName("resolveShouldIncludeCustomLensStylesheetWithHighestPrecedence")
    void resolveShouldIncludeCustomLensStylesheetWithHighestPrecedence() throws IOException {
        Path stylesheet = tempDir.resolve("my-theme.css");
        Files.writeString(stylesheet, "body {}");

        List<ResolvedLens> lenses = resolver.resolve(stylesheet.toFile(), List.of(), "");

        assertThat(lenses).extracting(ResolvedLens::id).contains("custom-lens");
    }

    @Test
    @DisplayName("resolveShouldFailWhenLensStylesheetCannotBeRead")
    void resolveShouldFailWhenLensStylesheetCannotBeRead() {
        Path notAFile = tempDir.resolve("a-directory");
        notAFile.toFile().mkdirs();

        assertThatThrownBy(() -> resolver.resolve(notAFile.toFile(), List.of(), ""))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("lensStylesheet");
    }

    @Test
    @DisplayName("resolveShouldRestrictExternalPacksToThePreferredOneWhenConfigured")
    void resolveShouldRestrictExternalPacksToThePreferredOneWhenConfigured() throws IOException {
        File preferred = writeStylePackJar("sunrise-theme.jar", "sunrise");
        File other = writeStylePackJar("midnight-theme.jar", "midnight");

        List<ResolvedLens> lenses = resolver.resolve(null, List.of(preferred, other), "com.example:sunrise-theme");

        assertThat(lenses).extracting(ResolvedLens::id).doesNotContain("midnight");
    }

    @Test
    @DisplayName("labelForShouldStripJarExtensionAndTrailingVersion")
    void labelForShouldStripJarExtensionAndTrailingVersion() {
        assertThat(resolver.labelFor(new File("midnight-theme-1.2.0.jar"))).isEqualTo("midnight-theme");
    }

    @Test
    @DisplayName("labelForShouldLeaveANameWithNoTrailingVersionUnchanged")
    void labelForShouldLeaveANameWithNoTrailingVersionUnchanged() {
        assertThat(resolver.labelFor(new File("midnight-theme.jar"))).isEqualTo("midnight-theme");
    }

    @Test
    @DisplayName("labelPartShouldReturnWholeCoordinateWhenNoColonPresent")
    void labelPartShouldReturnWholeCoordinateWhenNoColonPresent() {
        assertThat(resolver.labelPart("midnight-theme")).isEqualTo("midnight-theme");
    }

    @Test
    @DisplayName("labelPartShouldReturnArtifactPartWhenCoordinateHasAGroup")
    void labelPartShouldReturnArtifactPartWhenCoordinateHasAGroup() {
        assertThat(resolver.labelPart("com.example:midnight-theme")).isEqualTo("midnight-theme");
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
