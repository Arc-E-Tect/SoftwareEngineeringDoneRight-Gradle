package com.arc_e_tect.gradle.architecture;

import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assumptions;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GenerateArchitectureTestsTask")
class GenerateArchitectureTestsTaskTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("generateShouldWriteHexagonalArchitectureTestFromTemplate")
    void generateShouldWriteHexagonalArchitectureTestFromTemplate() throws Exception {
        GenerateArchitectureTestsTask task = ProjectBuilder.builder()
                .withProjectDir(tempDir.toFile())
                .build()
                .getTasks()
                .create("generateArchitectureTests", GenerateArchitectureTestsTask.class);

        task.getBasePackage().set("com.example.architecture");
        task.getInPorts().set(List.of("..application.port.in.."));
        task.getOutPorts().set(List.of("..application.port.out.."));
        task.getDomainModel().set(List.of("..application.domain.."));
        task.getAdapters().set(List.of("..adapter..", "..adapters.."));
        task.getApplicationServices().set(List.of("..application.service.."));
        task.getCommonPackages().set(List.of("..application.common.."));
        task.getFailOnDuplicateRules().set(false);
        task.getUserTestsDirectory().set(tempDir.resolve("user-tests").toFile());
        task.getOutputDirectory().set(tempDir.resolve("generated").toFile());

        task.generate();

        Path generatedFile = tempDir.resolve("generated/com/arc_e_tect/gradle/architecture/generated/HexagonalArchitectureTest.java");
        assertThat(generatedFile).exists();
        String contents = Files.readString(generatedFile);
        assertThat(contents)
                .contains("@AnalyzeClasses(packages = \"com.example.architecture\")")
                .contains("domain_must_not_depend_on_adapters")
                .contains("inbound_ports_reside_in_correct_package")
                .contains("application_services_must_not_depend_on_adapters")
                .contains("@ArchTest");
    }

    @Test
    @DisplayName("generateShouldCreateExternalRulePackSuiteWhenRulePackTestsExist")
    void generateShouldCreateExternalRulePackSuiteWhenRulePackTestsExist() throws Exception {
        GenerateArchitectureTestsTask task = ProjectBuilder.builder()
                .withProjectDir(tempDir.toFile())
                .build()
                .getTasks()
                .create("generateArchitectureTestsWithExternalRules", GenerateArchitectureTestsTask.class);

        Path fakeRulePackRoot = tempDir.resolve("rule-pack");
        Files.createDirectories(fakeRulePackRoot.resolve("com/example/rules"));
        Files.write(fakeRulePackRoot.resolve("com/example/rules/LayeredRulesTest.class"), new byte[] {0});

        task.getBasePackage().set("com.example.architecture");
        task.getInPorts().set(List.of("..application.port.in.."));
        task.getOutPorts().set(List.of("..application.port.out.."));
        task.getDomainModel().set(List.of("..application.domain.."));
        task.getAdapters().set(List.of("..adapter.."));
        task.getApplicationServices().set(List.of("..application.service.."));
        task.getCommonPackages().set(List.of("..application.common.."));
        task.getFailOnDuplicateRules().set(false);
        task.getUserTestsDirectory().set(tempDir.resolve("user-tests").toFile());
        task.getRulePackClasspath().from(fakeRulePackRoot.toFile());
        task.getOutputDirectory().set(tempDir.resolve("generated").toFile());

        task.generate();

        Path generatedSuite = tempDir.resolve("generated/com/arc_e_tect/gradle/architecture/generated/ExternalRulePackSuite.java");
        assertThat(generatedSuite).exists();
        assertThat(Files.readString(generatedSuite))
                .contains("@SelectPackages")
                .contains("com.example.rules")
                .contains("@IncludeClassNamePatterns({\".*Test\"})");
    }

        @Test
        @DisplayName("generateShouldSkipBuiltInHexagonalWhenDisabled")
        void generateShouldSkipBuiltInHexagonalWhenDisabled() {
                GenerateArchitectureTestsTask task = ProjectBuilder.builder()
                                .withProjectDir(tempDir.toFile())
                                .build()
                                .getTasks()
                                .create("generateArchitectureTestsWithoutBuiltIn", GenerateArchitectureTestsTask.class);

                task.getBasePackage().set("com.example.architecture");
                task.getInPorts().set(List.of("..application.port.in.."));
                task.getOutPorts().set(List.of("..application.port.out.."));
                task.getDomainModel().set(List.of("..application.domain.."));
                task.getAdapters().set(List.of("..adapter.."));
                task.getApplicationServices().set(List.of("..application.service.."));
                task.getCommonPackages().set(List.of("..application.common.."));
                task.getFailOnDuplicateRules().set(false);
                task.getUseBuiltInHexagonalRulePack().set(false);
                task.getUserTestsDirectory().set(tempDir.resolve("user-tests").toFile());
                task.getOutputDirectory().set(tempDir.resolve("generated").toFile());

                task.generate();

                Path generatedFile = tempDir.resolve("generated/com/arc_e_tect/gradle/architecture/generated/HexagonalArchitectureTest.java");
                assertThat(generatedFile).doesNotExist();
        }

        @Test
        @DisplayName("generateShouldCreateExternalRulePackSuiteWhenBuiltInHexagonalDisabled")
        void generateShouldCreateExternalRulePackSuiteWhenBuiltInHexagonalDisabled() throws Exception {
                GenerateArchitectureTestsTask task = ProjectBuilder.builder()
                                .withProjectDir(tempDir.toFile())
                                .build()
                                .getTasks()
                                .create("generateArchitectureTestsWithExternalRulesNoBuiltIn", GenerateArchitectureTestsTask.class);

                Path fakeRulePackRoot = tempDir.resolve("rule-pack-no-built-in");
                Files.createDirectories(fakeRulePackRoot.resolve("com/example/rules"));
                Files.write(fakeRulePackRoot.resolve("com/example/rules/LayeredRulesTest.class"), new byte[] {0});

                task.getBasePackage().set("com.example.architecture");
                task.getInPorts().set(List.of("..application.port.in.."));
                task.getOutPorts().set(List.of("..application.port.out.."));
                task.getDomainModel().set(List.of("..application.domain.."));
                task.getAdapters().set(List.of("..adapter.."));
                task.getApplicationServices().set(List.of("..application.service.."));
                task.getCommonPackages().set(List.of("..application.common.."));
                task.getFailOnDuplicateRules().set(false);
                task.getUseBuiltInHexagonalRulePack().set(false);
                task.getUserTestsDirectory().set(tempDir.resolve("user-tests").toFile());
                task.getRulePackClasspath().from(fakeRulePackRoot.toFile());
                task.getOutputDirectory().set(tempDir.resolve("generated").toFile());

                task.generate();

                Path generatedSuite = tempDir.resolve("generated/com/arc_e_tect/gradle/architecture/generated/ExternalRulePackSuite.java");
                assertThat(generatedSuite).exists();
        }

        @Test
        @DisplayName("generateShouldFailWhenDuplicateRulesDetectedAndFailOnDuplicateEnabled")
        void generateShouldFailWhenDuplicateRulesDetectedAndFailOnDuplicateEnabled() throws Exception {
                GenerateArchitectureTestsTask task = newTask("generateArchitectureTestsDuplicateFail");

                Path fakeRulePackRoot = tempDir.resolve("rule-pack-duplicates");
                Files.createDirectories(fakeRulePackRoot.resolve("com/example/rules"));
                Files.createDirectories(fakeRulePackRoot.resolve("com/other/rules"));
                Files.write(fakeRulePackRoot.resolve("com/example/rules/LayeredRulesTest.class"), new byte[] {0});
                Files.write(fakeRulePackRoot.resolve("com/other/rules/LayeredRulesTest.class"), new byte[] {0});

                configureDefaults(task, tempDir.resolve("generated"));
                task.getFailOnDuplicateRules().set(true);
                task.getRulePackClasspath().from(fakeRulePackRoot.toFile());

                assertThatThrownBy(task::generate)
                        .isInstanceOf(Exception.class)
                        .hasMessageContaining("Duplicate architecture rules discovered");
        }

        @Test
        @DisplayName("generateShouldContinueWhenDuplicateRulesDetectedAndFailOnDuplicateDisabled")
        void generateShouldContinueWhenDuplicateRulesDetectedAndFailOnDuplicateDisabled() throws Exception {
                GenerateArchitectureTestsTask task = newTask("generateArchitectureTestsDuplicateWarn");

                Path fakeRulePackRoot = tempDir.resolve("rule-pack-duplicates-warn");
                Files.createDirectories(fakeRulePackRoot.resolve("com/example/rules"));
                Files.createDirectories(fakeRulePackRoot.resolve("com/other/rules"));
                Files.write(fakeRulePackRoot.resolve("com/example/rules/LayeredRulesTest.class"), new byte[] {0});
                Files.write(fakeRulePackRoot.resolve("com/other/rules/LayeredRulesTest.class"), new byte[] {0});

                configureDefaults(task, tempDir.resolve("generated"));
                task.getFailOnDuplicateRules().set(false);
                task.getRulePackClasspath().from(fakeRulePackRoot.toFile());

                task.generate();

                Path generatedSuite = tempDir.resolve("generated/com/arc_e_tect/gradle/architecture/generated/ExternalRulePackSuite.java");
                assertThat(generatedSuite).exists();
                assertThat(Files.readString(generatedSuite)).contains("com.example.rules", "com.other.rules");
        }

        @Test
        @DisplayName("generateShouldCreateSuiteFromJarRulePack")
        void generateShouldCreateSuiteFromJarRulePack() throws Exception {
                GenerateArchitectureTestsTask task = newTask("generateArchitectureTestsFromJar");

                Path jarFile = tempDir.resolve("rule-pack.jar");
                try (JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(jarFile.toFile()))) {
                        jarOutputStream.putNextEntry(new JarEntry("com/example/rules/JarRulesTest.class"));
                        jarOutputStream.write(new byte[] {0});
                        jarOutputStream.closeEntry();

                        // Must be ignored because nested class
                        jarOutputStream.putNextEntry(new JarEntry("com/example/rules/JarRulesTest$Inner.class"));
                        jarOutputStream.write(new byte[] {0});
                        jarOutputStream.closeEntry();

                        // Must be ignored because framework package
                        jarOutputStream.putNextEntry(new JarEntry("org/junit/JunitRulesTest.class"));
                        jarOutputStream.write(new byte[] {0});
                        jarOutputStream.closeEntry();
                }

                configureDefaults(task, tempDir.resolve("generated"));
                task.getRulePackClasspath().from(jarFile.toFile());

                task.generate();

                Path generatedSuite = tempDir.resolve("generated/com/arc_e_tect/gradle/architecture/generated/ExternalRulePackSuite.java");
                assertThat(generatedSuite).exists();
                assertThat(Files.readString(generatedSuite)).contains("com.example.rules");
        }

        @Test
        @DisplayName("generateShouldNotCreateSuiteForDefaultPackageRuleTests")
        void generateShouldNotCreateSuiteForDefaultPackageRuleTests() throws Exception {
                GenerateArchitectureTestsTask task = newTask("generateArchitectureTestsDefaultPackage");

                Path fakeRulePackRoot = tempDir.resolve("rule-pack-default-package");
                Files.createDirectories(fakeRulePackRoot);
                Files.write(fakeRulePackRoot.resolve("RootLevelRulesTest.class"), new byte[] {0});

                configureDefaults(task, tempDir.resolve("generated"));
                task.getRulePackClasspath().from(fakeRulePackRoot.toFile());

                task.generate();

                Path generatedSuite = tempDir.resolve("generated/com/arc_e_tect/gradle/architecture/generated/ExternalRulePackSuite.java");
                assertThat(generatedSuite).doesNotExist();
        }

        @Test
        @DisplayName("generateShouldCleanStaleOutputBeforeRegenerating")
        void generateShouldCleanStaleOutputBeforeRegenerating() throws Exception {
                GenerateArchitectureTestsTask task = newTask("generateArchitectureTestsCleanOutput");

                Path outputRoot = tempDir.resolve("generated");
                Path staleFile = outputRoot.resolve("stale/OldFile.java");
                Files.createDirectories(staleFile.getParent());
                Files.writeString(staleFile, "stale");

                configureDefaults(task, outputRoot);

                task.generate();

                assertThat(staleFile).doesNotExist();
                Path generatedFile = outputRoot.resolve("com/arc_e_tect/gradle/architecture/generated/HexagonalArchitectureTest.java");
                assertThat(generatedFile).exists();
        }

        @Test
        @DisplayName("generateShouldCreateOutputDirectoryWhenItDoesNotExist")
        void generateShouldCreateOutputDirectoryWhenItDoesNotExist() {
                GenerateArchitectureTestsTask task = newTask("generateArchitectureTestsCreateOutput");

                Path outputRoot = tempDir.resolve("generated-missing-" + Instant.now().toEpochMilli());
                configureDefaults(task, outputRoot);

                task.generate();

                assertThat(outputRoot).exists().isDirectory();
                Path generatedFile = outputRoot.resolve("com/arc_e_tect/gradle/architecture/generated/HexagonalArchitectureTest.java");
                assertThat(generatedFile).exists();
        }

        @Test
        @DisplayName("generateShouldIgnoreMissingRulePackClasspathEntry")
        void generateShouldIgnoreMissingRulePackClasspathEntry() {
                GenerateArchitectureTestsTask task = newTask("generateArchitectureTestsMissingClasspathEntry");

                Path outputRoot = tempDir.resolve("generated-missing-classpath");
                configureDefaults(task, outputRoot);
                task.getRulePackClasspath().from(tempDir.resolve("does-not-exist").toFile());

                task.generate();

                Path generatedSuite = outputRoot.resolve("com/arc_e_tect/gradle/architecture/generated/ExternalRulePackSuite.java");
                assertThat(generatedSuite).doesNotExist();
                Path generatedHexagonal = outputRoot.resolve("com/arc_e_tect/gradle/architecture/generated/HexagonalArchitectureTest.java");
                assertThat(generatedHexagonal).exists();
        }

        @Test
        @DisplayName("generateShouldFailForBuiltInAndExternalDuplicateRuleName")
        void generateShouldFailForBuiltInAndExternalDuplicateRuleName() throws Exception {
                GenerateArchitectureTestsTask task = newTask("generateArchitectureTestsBuiltInDuplicate");

                Path fakeRulePackRoot = tempDir.resolve("rule-pack-built-in-duplicate");
                Files.createDirectories(fakeRulePackRoot.resolve("com/example/rules"));
                Files.write(fakeRulePackRoot.resolve("com/example/rules/HexagonalArchitectureTest.class"), new byte[] {0});

                configureDefaults(task, tempDir.resolve("generated"));
                task.getFailOnDuplicateRules().set(true);
                task.getRulePackClasspath().from(fakeRulePackRoot.toFile());

                assertThatThrownBy(task::generate)
                        .isInstanceOf(Exception.class)
                        .hasMessageContaining("Duplicate architecture rules discovered");
        }

        @Test
        @DisplayName("generateShouldFailWhenLocalAndExternalRulesShareSimpleName")
        void generateShouldFailWhenLocalAndExternalRulesShareSimpleName() throws Exception {
                GenerateArchitectureTestsTask task = newTask("generateArchitectureTestsLocalDuplicate");

                Path userTestsDir = tempDir.resolve("user-tests");
                Files.createDirectories(userTestsDir.resolve("com/example/local"));
                Files.writeString(userTestsDir.resolve("com/example/local/LayeredRulesTest.java"), "class LayeredRulesTest {}\n");

                Path fakeRulePackRoot = tempDir.resolve("rule-pack-local-duplicate");
                Files.createDirectories(fakeRulePackRoot.resolve("com/example/rules"));
                Files.write(fakeRulePackRoot.resolve("com/example/rules/LayeredRulesTest.class"), new byte[] {0});

                configureDefaults(task, tempDir.resolve("generated"));
                task.getUserTestsDirectory().set(userTestsDir.toFile());
                task.getFailOnDuplicateRules().set(true);
                task.getRulePackClasspath().from(fakeRulePackRoot.toFile());

                assertThatThrownBy(task::generate)
                        .isInstanceOf(Exception.class)
                        .hasMessageContaining("Duplicate architecture rules discovered");
        }

        @Test
        @DisplayName("generateShouldFailWhenUserTestsDirectoryIsUnreadable")
        void generateShouldFailWhenUserTestsDirectoryIsUnreadable() throws Exception {
                GenerateArchitectureTestsTask task = newTask("generateArchitectureTestsUnreadableUserTests");

                Path userTestsDir = tempDir.resolve("unreadable-user-tests");
                Files.createDirectories(userTestsDir);

                Assumptions.assumeTrue(Files.getFileStore(userTestsDir).supportsFileAttributeView("posix"));
                Set<PosixFilePermission> originalPermissions = Files.getPosixFilePermissions(userTestsDir);
                Files.setPosixFilePermissions(userTestsDir, Set.of());

                try {
                        configureDefaults(task, tempDir.resolve("generated"));
                        task.getUserTestsDirectory().set(userTestsDir.toFile());

                        assertThatThrownBy(task::generate)
                                .isInstanceOf(Exception.class)
                                .hasMessageContaining("Failed to inspect local architecture tests");
                } finally {
                        Files.setPosixFilePermissions(userTestsDir, originalPermissions);
                }
        }

        @Test
        @DisplayName("generateShouldFailWhenOutputDirectoryCannotBeReset")
        void generateShouldFailWhenOutputDirectoryCannotBeReset() throws Exception {
                GenerateArchitectureTestsTask task = newTask("generateArchitectureTestsUnreadableOutput");

                Path outputRoot = tempDir.resolve("unreadable-output");
                Files.createDirectories(outputRoot.resolve("nested"));
                Files.writeString(outputRoot.resolve("nested/stale.txt"), "stale");

                Assumptions.assumeTrue(Files.getFileStore(outputRoot).supportsFileAttributeView("posix"));
                Set<PosixFilePermission> originalPermissions = Files.getPosixFilePermissions(outputRoot);
                Files.setPosixFilePermissions(outputRoot, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE));

                try {
                        configureDefaults(task, outputRoot);

                        assertThatThrownBy(task::generate)
                                .isInstanceOf(Exception.class)
                                .hasMessageContaining("Failed to reset generated architecture test directory");
                } finally {
                        Files.setPosixFilePermissions(outputRoot, originalPermissions);
                }
        }

        private GenerateArchitectureTestsTask newTask(String taskName) {
                return ProjectBuilder.builder()
                                .withProjectDir(tempDir.toFile())
                                .build()
                                .getTasks()
                                .create(taskName, GenerateArchitectureTestsTask.class);
        }

        private void configureDefaults(GenerateArchitectureTestsTask task, Path outputRoot) {
                task.getBasePackage().set("com.example.architecture");
                task.getInPorts().set(List.of("..application.port.in.."));
                task.getOutPorts().set(List.of("..application.port.out.."));
                task.getDomainModel().set(List.of("..application.domain.."));
                task.getAdapters().set(List.of("..adapter.."));
                task.getApplicationServices().set(List.of("..application.service.."));
                task.getCommonPackages().set(List.of("..application.common.."));
                task.getFailOnDuplicateRules().set(false);
                task.getUseBuiltInHexagonalRulePack().set(true);
                task.getUserTestsDirectory().set(tempDir.resolve("user-tests").toFile());
                task.getOutputDirectory().set(outputRoot.toFile());
        }
}