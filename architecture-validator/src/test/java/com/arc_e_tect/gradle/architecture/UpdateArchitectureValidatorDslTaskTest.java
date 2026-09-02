package com.arc_e_tect.gradle.architecture;

import org.gradle.api.GradleException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UpdateArchitectureValidatorDslTask")
class UpdateArchitectureValidatorDslTaskTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("updateDslShouldFailWhenTheBuildFileDoesNotExist")
    void updateDslShouldFailWhenTheBuildFileDoesNotExist() {
        UpdateArchitectureValidatorDslTask task = newTask();
        task.getBuildFile().set(tempDir.resolve("build.gradle").toFile());

        assertThatThrownBy(task::updateDsl)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("build file not found");
    }

    @Test
    @DisplayName("updateDslShouldLeaveAMissingBlockAloneWhenGenerateDslIsNotSet")
    void updateDslShouldLeaveAMissingBlockAloneWhenGenerateDslIsNotSet() throws Exception {
        String original = "plugins {\n    id 'com.arc-e-tect.architecture-validator'\n}\n";
        Path buildFile = writeBuildFile(original);
        UpdateArchitectureValidatorDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        assertThat(Files.readString(buildFile)).isEqualTo(original);
        assertThat(backupFileFor(buildFile)).doesNotExist();
    }

    @Test
    @DisplayName("updateDslShouldGenerateAFullBlockWhenGenerateDslIsSetAndNoneExists")
    void updateDslShouldGenerateAFullBlockWhenGenerateDslIsSetAndNoneExists() throws Exception {
        Path buildFile = writeBuildFile("plugins {\n    id 'com.arc-e-tect.architecture-validator'\n}\n");
        UpdateArchitectureValidatorDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());
        task.applyGenerateDsl(true);

        task.updateDsl();

        String updated = Files.readString(buildFile);
        assertThat(updated).contains("architectureValidator {");
        assertThat(updated).contains("testDirectory = layout.projectDirectory.dir('src/testArchitecture/java')");
        assertThat(updated).contains("failOnViolation = true");
        assertThat(updated).contains("maxAllowedViolations = 0");
        assertThat(updated).contains("ignoreFailures = false");
        assertThat(updated).contains("failOnDuplicateRules = false");
        assertThat(updated).contains("useBuiltInHexagonalRulePack = true");
        assertThat(updated).contains("rulesDisabled = []");
        // junitVersion is deliberately excluded - see the schema's own javadoc.
        assertThat(updated).doesNotContain("junitVersion");
        assertThat(updated).contains("hexagonalArchitecture {");
        assertThat(updated).contains("inPorts = ['..application.port.inbound..']");
        assertThat(updated).contains("namingConventionsEnabled = false");
    }

    @Test
    @DisplayName("updateDslShouldAddOnlyMissingScalarPropertiesToAnExistingBlockAndNeverTouchHexagonalArchitecture")
    void updateDslShouldAddOnlyMissingScalarPropertiesToAnExistingBlockAndNeverTouchHexagonalArchitecture() throws Exception {
        Path buildFile = writeBuildFile("architectureValidator {\n"
                + "    failOnViolation = false\n"
                + "}\n");
        UpdateArchitectureValidatorDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        String updated = Files.readString(buildFile);
        // Existing value, including the deliberately-non-default one, is never touched.
        assertThat(updated).contains("failOnViolation = false");
        assertThat(updated).contains("testDirectory = layout.projectDirectory.dir('src/testArchitecture/java')");
        assertThat(updated).contains("rulesDisabled = []");
        // hexagonalArchitecture is a container: never added to an existing block.
        assertThat(updated).doesNotContain("hexagonalArchitecture");
    }

    @Test
    @DisplayName("updateDslShouldBeANoOpWhenTheBlockIsAlreadyFullyConfigured")
    void updateDslShouldBeANoOpWhenTheBlockIsAlreadyFullyConfigured() throws Exception {
        String original = "architectureValidator {\n"
                + "    testDirectory = layout.projectDirectory.dir('src/testArchitecture/java')\n"
                + "    basePackage = project.provider { project.group == null || project.group.toString() == "
                + "'unspecified' ? '' : project.group.toString() }\n"
                + "    failOnViolation = true\n"
                + "    maxAllowedViolations = 0\n"
                + "    ignoreFailures = false\n"
                + "    failOnDuplicateRules = false\n"
                + "    useBuiltInHexagonalRulePack = true\n"
                + "    rulesDisabled = []\n"
                + "}\n";
        Path buildFile = writeBuildFile(original);
        UpdateArchitectureValidatorDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        assertThat(Files.readString(buildFile)).isEqualTo(original);
        assertThat(backupFileFor(buildFile)).doesNotExist();
    }

    @Test
    @DisplayName("updateDslShouldBackUpTheOriginalFileBeforeWritingChanges")
    void updateDslShouldBackUpTheOriginalFileBeforeWritingChanges() throws Exception {
        String original = "architectureValidator {\n    failOnViolation = false\n}\n";
        Path buildFile = writeBuildFile(original);
        UpdateArchitectureValidatorDslTask task = newTask();
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
        Path buildFile = writeBuildFile("architectureValidator {\n"
                + "    // why this value was chosen\n"
                + "    failOnViolation = false\n"
                + "}\n");
        UpdateArchitectureValidatorDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());
        task.applyCleanupDsl(true);

        task.updateDsl();

        String updated = Files.readString(buildFile);
        assertThat(updated).doesNotContain("//");
        assertThat(updated).contains("failOnViolation = false");
    }

    @Test
    @DisplayName("generateDslWithCleanupDslShouldOmitTheHexagonalArchitectureStubEntirely")
    void generateDslWithCleanupDslShouldOmitTheHexagonalArchitectureStubEntirely() throws Exception {
        Path buildFile = writeBuildFile("plugins {\n    id 'com.arc-e-tect.architecture-validator'\n}\n");
        UpdateArchitectureValidatorDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());
        task.applyGenerateDsl(true);
        task.applyCleanupDsl(true);

        task.updateDsl();

        String updated = Files.readString(buildFile);
        assertThat(updated).doesNotContain("//");
        assertThat(updated).contains("hexagonalArchitecture {");
        // Empty stub under cleanup - still behaviorally a no-op since the nested extension's own
        // conventions apply regardless of whether the DSL text shows them.
        assertThat(updated).doesNotContain("inPorts");
    }

    @Test
    @DisplayName("updateDslShouldNeverAddJunitVersionEvenWhenConfiguredOnlyViaTheSetJunitVersionMethodForm")
    void updateDslShouldNeverAddJunitVersionEvenWhenConfiguredOnlyViaTheSetJunitVersionMethodForm() throws Exception {
        // Real-world regression case: a project pulling the version from a catalog configures
        // junitVersion via the plain setJunitVersion(String) method, not junitVersion = '...'.
        // junitVersion is excluded from the schema for exactly this reason - see
        // ArchitectureValidatorDslSchema's own javadoc for why appending junitVersion = '6.1.0'
        // here would silently override this real configured value.
        String original = "architectureValidator {\n"
                + "    setJunitVersion(libs.versions.junit.get())\n"
                + "}\n";
        Path buildFile = writeBuildFile(original);
        UpdateArchitectureValidatorDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        String updated = Files.readString(buildFile);
        assertThat(updated).contains("setJunitVersion(libs.versions.junit.get())");
        assertThat(updated).doesNotContain("junitVersion =");
    }

    private Path writeBuildFile(String content) throws Exception {
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, content);
        return buildFile;
    }

    private Path backupFileFor(Path buildFile) {
        return buildFile.resolveSibling(buildFile.getFileName() + ".bak");
    }

    private UpdateArchitectureValidatorDslTask newTask() {
        return ProjectBuilder.builder()
                .withProjectDir(tempDir.toFile())
                .build()
                .getTasks()
                .create("updateArchitectureValidatorDSL", UpdateArchitectureValidatorDslTask.class);
    }
}
