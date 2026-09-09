package com.arc_e_tect.gradle.gherkin.indexing;

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

@DisplayName("ProjectAttribution")
class ProjectAttributionTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("attributes a feature file to the project directory enclosing it")
    void attributesFileToItsEnclosingProject() throws IOException {
        File catalog = projectDir("catalog");
        File feature = featureIn(catalog, "catalog.feature");

        ProjectAttribution attribution = new ProjectAttribution(List.of(catalog));

        assertThat(attribution.owningProjectDirectory(feature)).isEqualTo(catalog);
    }

    @Test
    @DisplayName("attributes a feature file to the most specific enclosing project, not the root project above it")
    void prefersTheMostSpecificEnclosingProject() throws IOException {
        File root = tempDir.toFile();
        File catalog = projectDir("catalog");
        File feature = featureIn(catalog, "catalog.feature");

        ProjectAttribution attribution = new ProjectAttribution(List.of(root, catalog));

        assertThat(attribution.owningProjectDirectory(feature)).isEqualTo(catalog);
    }

    @Test
    @DisplayName("prefers the deeper project even when a shallower one spells its path at greater length")
    void ranksBySpecificityNotRawPathLength() throws IOException {
        // The root project given by its canonical path, the sub-project by the symlinked one it was
        // configured with - the shape ProjectBuilder produces on macOS, where /var is a symlink to
        // /private/var. The root's raw path is then the longer string, but the sub-project is still
        // the more specific owner of files beneath it.
        File root = tempDir.toFile().getCanonicalFile();
        File catalog = new File(tempDir.toFile().getAbsoluteFile(), "catalog");
        catalog.mkdirs();
        File feature = featureIn(catalog, "catalog.feature");

        ProjectAttribution attribution = new ProjectAttribution(List.of(root, catalog));

        assertThat(attribution.owningProjectDirectory(feature)).isEqualTo(catalog);
    }

    @Test
    @DisplayName("attributes a feature file under none of the project directories to itself")
    void attributesUnmatchedFileToItself() throws IOException {
        File catalog = projectDir("catalog");
        File orphanDir = new File(tempDir.toFile(), "elsewhere");
        orphanDir.mkdirs();
        File orphan = featureIn(orphanDir, "orphan.feature");

        ProjectAttribution attribution = new ProjectAttribution(List.of(catalog));

        assertThat(attribution.owningProjectDirectory(orphan)).isEqualTo(orphan);
    }

    @Test
    @DisplayName("attributes every file to itself when no project directories are given")
    void attributesEveryFileToItselfWithoutProjectDirectories() throws IOException {
        File catalog = projectDir("catalog");
        File feature = featureIn(catalog, "catalog.feature");

        ProjectAttribution attribution = new ProjectAttribution(List.of());

        assertThat(attribution.owningProjectDirectory(feature)).isEqualTo(feature);
    }

    @Test
    @DisplayName("collects one owner when every file belongs to the same project")
    void collectsOneOwnerForOneProject() throws IOException {
        File catalog = projectDir("catalog");
        File first = featureIn(catalog, "first.feature");
        File second = featureIn(catalog, "second.feature");

        ProjectAttribution attribution = new ProjectAttribution(List.of(catalog));

        assertThat(attribution.owningProjectDirectories(List.of(first, second))).containsExactly(catalog);
    }

    @Test
    @DisplayName("collects every distinct owner, in the order each was first encountered")
    void collectsEveryDistinctOwnerInEncounterOrder() throws IOException {
        File catalog = projectDir("catalog");
        File checkout = projectDir("checkout");
        File catalogFeature = featureIn(catalog, "catalog.feature");
        File checkoutFeature = featureIn(checkout, "checkout.feature");
        File secondCatalogFeature = featureIn(catalog, "catalog-extra.feature");

        ProjectAttribution attribution = new ProjectAttribution(List.of(catalog, checkout));

        assertThat(attribution.owningProjectDirectories(
                List.of(checkoutFeature, catalogFeature, secondCatalogFeature)))
                .containsExactly(checkout, catalog);
    }

    @Test
    @DisplayName("collects no owners for no files")
    void collectsNoOwnersForNoFiles() throws IOException {
        ProjectAttribution attribution = new ProjectAttribution(List.of(projectDir("catalog")));

        assertThat(attribution.owningProjectDirectories(List.of())).isEmpty();
    }

    private File projectDir(String name) {
        File dir = new File(tempDir.toFile(), name);
        dir.mkdirs();
        return dir;
    }

    private File featureIn(File dir, String name) throws IOException {
        File file = new File(dir, name);
        Files.writeString(file.toPath(), "Feature: " + name + "\n", StandardCharsets.UTF_8);
        return file;
    }
}
