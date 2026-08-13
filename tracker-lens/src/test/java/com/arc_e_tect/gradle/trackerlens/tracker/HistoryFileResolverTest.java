package com.arc_e_tect.gradle.trackerlens.tracker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HistoryFileResolver")
class HistoryFileResolverTest {

    @TempDir
    Path tempDir;

    private final HistoryFileResolver resolver = new HistoryFileResolver();

    @Test
    @DisplayName("resolveShouldPassThroughAnIndividualFileUnchanged")
    void resolveShouldPassThroughAnIndividualFileUnchanged() {
        File file = tempDir.resolve("history.ndjson").toFile();

        List<File> resolved = resolver.resolve(List.of(file));

        assertThat(resolved).containsExactly(file);
    }

    @Test
    @DisplayName("resolveShouldExpandADirectoryToItsNdjsonChildrenSortedByName")
    void resolveShouldExpandADirectoryToItsNdjsonChildrenSortedByName() throws IOException {
        Path dir = tempDir.resolve("histories");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("zeta.ndjson"), "");
        Files.writeString(dir.resolve("alpha.ndjson"), "");
        Files.writeString(dir.resolve("notes.txt"), "");

        List<File> resolved = resolver.resolve(List.of(dir.toFile()));

        assertThat(resolved).extracting(File::getName).containsExactly("alpha.ndjson", "zeta.ndjson");
    }

    @Test
    @DisplayName("resolveShouldMixIndividualFilesAndDirectoriesInOneCall")
    void resolveShouldMixIndividualFilesAndDirectoriesInOneCall() throws IOException {
        Path dir = tempDir.resolve("histories");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("shadow.ndjson"), "");
        File standaloneFile = tempDir.resolve("mirage.ndjson").toFile();

        List<File> resolved = resolver.resolve(List.of(dir.toFile(), standaloneFile));

        assertThat(resolved).extracting(File::getName).containsExactly("shadow.ndjson", "mirage.ndjson");
    }

    @Test
    @DisplayName("resolveShouldReturnEmptyForADirectoryWithNoNdjsonFiles")
    void resolveShouldReturnEmptyForADirectoryWithNoNdjsonFiles() throws IOException {
        Path dir = tempDir.resolve("empty-histories");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("readme.md"), "");

        List<File> resolved = resolver.resolve(List.of(dir.toFile()));

        assertThat(resolved).isEmpty();
    }
}
