package com.arc_e_tect.gradle.trackerlens.lens;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TemplateSetResolver")
class TemplateSetResolverTest {

    @TempDir
    Path tempDir;

    private final TemplateSetResolver resolver = new TemplateSetResolver();

    @Test
    @DisplayName("resolveShouldAlwaysIncludeTheBuiltInTemplate")
    void resolveShouldAlwaysIncludeTheBuiltInTemplate() {
        List<ResolvedTemplate> templates = resolver.resolve(List.of(), "");

        assertThat(templates).extracting(ResolvedTemplate::id).containsExactly("default");
    }

    @Test
    @DisplayName("resolveShouldIncludeExternalPackTemplatesFromClasspathEntries")
    void resolveShouldIncludeExternalPackTemplatesFromClasspathEntries() throws IOException {
        File jarFile = writeTemplatePackJar("venn-view-pack.jar", "venn-diagram-view");

        List<ResolvedTemplate> templates = resolver.resolve(List.of(jarFile), "");

        assertThat(templates).extracting(ResolvedTemplate::id).contains("venn-diagram-view");
    }

    @Test
    @DisplayName("resolveShouldSkipClasspathEntriesThatDoNotExist")
    void resolveShouldSkipClasspathEntriesThatDoNotExist() {
        File missing = tempDir.resolve("does-not-exist.jar").toFile();

        List<ResolvedTemplate> templates = resolver.resolve(List.of(missing), "");

        assertThat(templates).extracting(ResolvedTemplate::id).containsExactly("default");
    }

    @Test
    @DisplayName("resolveShouldRestrictExternalPacksToThePreferredOneWhenConfigured")
    void resolveShouldRestrictExternalPacksToThePreferredOneWhenConfigured() throws IOException {
        File preferred = writeTemplatePackJar("sunrise-view-pack.jar", "sunrise-view");
        File other = writeTemplatePackJar("venn-view-pack.jar", "venn-diagram-view");

        List<ResolvedTemplate> templates =
                resolver.resolve(List.of(preferred, other), "com.example:sunrise-view-pack");

        assertThat(templates).extracting(ResolvedTemplate::id).doesNotContain("venn-diagram-view");
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
