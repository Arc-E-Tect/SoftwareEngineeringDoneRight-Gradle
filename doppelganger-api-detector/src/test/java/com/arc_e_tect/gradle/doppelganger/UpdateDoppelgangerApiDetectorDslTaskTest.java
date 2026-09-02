package com.arc_e_tect.gradle.doppelganger;

import org.gradle.api.GradleException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UpdateDoppelgangerApiDetectorDslTask")
class UpdateDoppelgangerApiDetectorDslTaskTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("updateDslShouldFailWhenTheBuildFileDoesNotExist")
    void updateDslShouldFailWhenTheBuildFileDoesNotExist() {
        UpdateDoppelgangerApiDetectorDslTask task = newTask();
        task.getBuildFile().set(tempDir.resolve("build.gradle").toFile());

        assertThatThrownBy(task::updateDsl)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("build file not found");
    }

    @Test
    @DisplayName("updateDslShouldLeaveAMissingBlockAloneWhenGenerateDslIsNotSet")
    void updateDslShouldLeaveAMissingBlockAloneWhenGenerateDslIsNotSet() throws Exception {
        String original = "plugins {\n    id 'com.arc-e-tect.doppelganger-api-detector'\n}\n";
        Path buildFile = writeBuildFile(original);
        UpdateDoppelgangerApiDetectorDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        assertThat(Files.readString(buildFile)).isEqualTo(original);
        assertThat(backupFileFor(buildFile)).doesNotExist();
    }

    @Test
    @DisplayName("updateDslShouldGenerateAFullBlockWhenGenerateDslIsSetAndNoneExists")
    void updateDslShouldGenerateAFullBlockWhenGenerateDslIsSetAndNoneExists() throws Exception {
        Path buildFile = writeBuildFile("plugins {\n    id 'com.arc-e-tect.doppelganger-api-detector'\n}\n");
        UpdateDoppelgangerApiDetectorDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());
        task.applyGenerateDsl(true);

        task.updateDsl();

        String updated = Files.readString(buildFile);
        assertThat(updated).contains("doppelgangerApiDetector {");
        assertThat(updated).contains("failOnDoppelganger = false");
        assertThat(updated).contains("useRestDocs = true");
        assertThat(updated).contains("useOpenApiRequestValidator = false");
        assertThat(updated).contains("useSpringCloudContract = false");
        assertThat(updated).contains("reportDir = layout.buildDirectory.dir('reports/doppelganger-api-detector')");
        assertThat(updated).contains("reportFileName = 'doppelganger-apis.adoc'");
        assertThat(updated).contains("systemUnderTestVersion = project.version.toString()");
        assertThat(updated).contains("openApiDir = layout.dir(rootDocument.map { it.asFile.parentFile })");
        assertThat(updated).contains(
                "contractHistoryFile = file('doppelganger-api-detector-contract-history.ndjson')");
        assertThat(updated).contains("updateContractHistory = trackContractHistory");
        assertThat(updated).contains("excludePaths = []");
        assertThat(updated).contains("excludeWellKnown = []");
        assertThat(updated).contains("pathResolverHelperMethods = []");
        assertThat(updated).contains("includeResponseCoverage = false");
        assertThat(updated).contains("ignore5xx = false");
        assertThat(updated).contains("scanContractsReportFileName = 'contract-coverage.adoc'");
        assertThat(updated).contains("trackResponseCoverageHistory = false");
        assertThat(updated).contains(
                "responseCoverageHistoryFile = file('doppelganger-api-detector-response-coverage-history.ndjson')");
        assertThat(updated).contains("updateResponseCoverageHistory = trackResponseCoverageHistory");
    }

    @Test
    @DisplayName("updateDslShouldAddOnlyMissingScalarPropertiesToAnExistingBlock")
    void updateDslShouldAddOnlyMissingScalarPropertiesToAnExistingBlock() throws Exception {
        Path buildFile = writeBuildFile("doppelgangerApiDetector {\n"
                + "    rootDocument = file('src/main/resources/openapi/openapi.yaml')\n"
                + "    useRestDocs = false\n"
                + "}\n");
        UpdateDoppelgangerApiDetectorDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        String updated = Files.readString(buildFile);
        // Existing values, including the deliberately-non-default one, are never touched.
        assertThat(updated).contains("useRestDocs = false");
        assertThat(updated).contains("reportDir = layout.buildDirectory.dir('reports/doppelganger-api-detector')");
        assertThat(updated).contains("systemUnderTestVersion = project.version.toString()");
    }

    @Test
    @DisplayName("updateDslShouldBeANoOpWhenTheBlockIsAlreadyFullyConfigured")
    void updateDslShouldBeANoOpWhenTheBlockIsAlreadyFullyConfigured() throws Exception {
        String original = "doppelgangerApiDetector {\n"
                + "    rootDocument = file('src/main/resources/openapi/openapi.yaml')\n"
                + "    failOnDoppelganger = false\n"
                + "    useRestDocs = true\n"
                + "    useOpenApiRequestValidator = false\n"
                + "    useSpringCloudContract = false\n"
                + "    reportDir = layout.buildDirectory.dir('reports/doppelganger-api-detector')\n"
                + "    reportFileName = 'doppelganger-apis.adoc'\n"
                + "    systemUnderTestVersion = project.version.toString()\n"
                + "    openApiDir = layout.dir(rootDocument.map { it.asFile.parentFile })\n"
                + "    trackContractHistory = false\n"
                + "    contractHistoryFile = file('doppelganger-api-detector-contract-history.ndjson')\n"
                + "    updateContractHistory = trackContractHistory\n"
                + "    excludePaths = []\n"
                + "    excludeWellKnown = []\n"
                + "    pathResolverHelperMethods = []\n"
                + "    includeResponseCoverage = false\n"
                + "    ignore5xx = false\n"
                + "    scanContractsReportFileName = 'contract-coverage.adoc'\n"
                + "    trackResponseCoverageHistory = false\n"
                + "    responseCoverageHistoryFile = "
                + "file('doppelganger-api-detector-response-coverage-history.ndjson')\n"
                + "    updateResponseCoverageHistory = trackResponseCoverageHistory\n"
                + "}\n";
        Path buildFile = writeBuildFile(original);
        UpdateDoppelgangerApiDetectorDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        assertThat(Files.readString(buildFile)).isEqualTo(original);
        assertThat(backupFileFor(buildFile)).doesNotExist();
    }

    @Test
    @DisplayName("updateDslShouldBackUpTheOriginalFileBeforeWritingChanges")
    void updateDslShouldBackUpTheOriginalFileBeforeWritingChanges() throws Exception {
        String original = "doppelgangerApiDetector {\n    useRestDocs = false\n}\n";
        Path buildFile = writeBuildFile(original);
        UpdateDoppelgangerApiDetectorDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        Path backup = backupFileFor(buildFile);
        assertThat(backup).exists();
        assertThat(Files.readString(backup)).isEqualTo(original);
        assertThat(Files.readString(buildFile)).isNotEqualTo(original);
    }

    @Test
    @DisplayName("updateDslWithCleanupDslShouldStripCommentsFromAnExistingBlock")
    void updateDslWithCleanupDslShouldStripCommentsFromAnExistingBlock() throws Exception {
        Path buildFile = writeBuildFile("doppelgangerApiDetector {\n"
                + "    // why this value was chosen\n"
                + "    useRestDocs = false\n"
                + "}\n");
        UpdateDoppelgangerApiDetectorDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());
        task.applyCleanupDsl(true);

        task.updateDsl();

        String updated = Files.readString(buildFile);
        assertThat(updated).doesNotContain("//");
        assertThat(updated).contains("useRestDocs = false");
    }

    private Path writeBuildFile(String content) throws Exception {
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, content);
        return buildFile;
    }

    private Path backupFileFor(Path buildFile) {
        return buildFile.resolveSibling(buildFile.getFileName() + ".bak");
    }

    private UpdateDoppelgangerApiDetectorDslTask newTask() {
        return ProjectBuilder.builder()
                .withProjectDir(tempDir.toFile())
                .build()
                .getTasks()
                .create("updateDoppelgangerApiDetectorDSL", UpdateDoppelgangerApiDetectorDslTask.class);
    }
}
