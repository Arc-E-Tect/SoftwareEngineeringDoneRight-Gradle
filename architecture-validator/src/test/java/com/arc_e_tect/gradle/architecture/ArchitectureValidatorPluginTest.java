package com.arc_e_tect.gradle.architecture;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ArchitectureValidatorPlugin")
class ArchitectureValidatorPluginTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("pluginShouldRegisterGenerateArchitectureTestsTask")
    void pluginShouldRegisterGenerateArchitectureTestsTask() {
        Project project = projectWithPlugin();

        assertThat(project.getTasks().findByName(ArchitectureValidatorPlugin.GENERATE_ARCHITECTURE_TESTS_TASK_NAME))
                .isNotNull();
    }

    @Test
    @DisplayName("pluginShouldRegisterArchitectureValidatorExtensionDefaults")
    void pluginShouldRegisterArchitectureValidatorExtensionDefaults() {
        Project project = projectWithPlugin();

        ArchitectureValidatorExtension extension = project.getExtensions().getByType(ArchitectureValidatorExtension.class);

        assertThat(extension.getFailOnViolation().get()).isTrue();
        assertThat(extension.getMaxAllowedViolations().get()).isZero();
        assertThat(extension.getIgnoreFailures().get()).isFalse();
        assertThat(extension.getFailOnDuplicateRules().get()).isFalse();
        assertThat(extension.getUseBuiltInHexagonalRulePack().get()).isTrue();
        assertThat(extension.getRulesDisabled().get()).isEmpty();
        assertThat(extension.getHexagonalArchitecture().getNamingConventionsEnabled().get()).isFalse();
    }

    @Test
    @DisplayName("hexagonalArchitectureActionShouldConfigureNestedExtension")
    void hexagonalArchitectureActionShouldConfigureNestedExtension() {
        Project project = projectWithPlugin();
        ArchitectureValidatorExtension extension = project.getExtensions().getByType(ArchitectureValidatorExtension.class);

        extension.hexagonalArchitecture(hex -> hex.getInPorts().set(java.util.List.of("..inbound..")));

        assertThat(extension.getHexagonalArchitecture().getInPorts().get()).containsExactly("..inbound..");
    }

    @Test
    @DisplayName("architectureValidationTestListenerShouldTrackOnlyRootSuiteFailures")
    void architectureValidationTestListenerShouldTrackOnlyRootSuiteFailures() {
        AtomicLong failedTests = new AtomicLong(0L);
        ArchitectureValidatorPlugin.ArchitectureValidationTestListener listener =
                new ArchitectureValidatorPlugin.ArchitectureValidationTestListener(failedTests);

        TestDescriptor rootSuite = proxyTestDescriptor(null);
        TestDescriptor childSuite = proxyTestDescriptor(rootSuite);

        listener.beforeSuite(rootSuite);
        listener.beforeTest(childSuite);
        listener.afterTest(childSuite, proxyTestResult(4L));
        listener.afterSuite(childSuite, proxyTestResult(3L));

        assertThat(failedTests.get()).isZero();

        listener.afterSuite(rootSuite, proxyTestResult(2L));
        assertThat(failedTests.get()).isEqualTo(2L);
    }

    private Project projectWithPlugin() {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply(ArchitectureValidatorPlugin.class);
        return project;
    }

    private TestDescriptor proxyTestDescriptor(TestDescriptor parent) {
        return (TestDescriptor) java.lang.reflect.Proxy.newProxyInstance(
                TestDescriptor.class.getClassLoader(),
                new Class<?>[] {TestDescriptor.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getParent" -> parent;
                    case "toString" -> "TestDescriptorProxy";
                    default -> null;
                });
    }

    private TestResult proxyTestResult(long failedTests) {
        return (TestResult) java.lang.reflect.Proxy.newProxyInstance(
                TestResult.class.getClassLoader(),
                new Class<?>[] {TestResult.class},
                (proxy, method, args) -> {
                    if ("getFailedTestCount".equals(method.getName())) {
                        return failedTests;
                    }
                    if ("toString".equals(method.getName())) {
                        return "TestResultProxy";
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == long.class) {
                        return 0L;
                    }
                    if (returnType == int.class) {
                        return 0;
                    }
                    return null;
                });
    }
}