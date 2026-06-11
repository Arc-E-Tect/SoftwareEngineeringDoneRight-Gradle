package com.arc_e_tect.gradle.architecture;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.TestResult;
import org.gradle.testing.base.TestingExtension;

import com.arc_e_tect.sedr.utils.jacoco.marker.ExcludeFromJacocoGeneratedCodeCoverage;

import java.util.concurrent.atomic.AtomicLong;

@ExcludeFromJacocoGeneratedCodeCoverage(justification = "Gradle plugin wiring — requires GradleTestKit to exercise")
public class ArchitectureValidatorPlugin implements Plugin<Project> {

    public static final String TEST_ARCHITECTURE_TASK_NAME = "testArchitecture";
    public static final String GENERATE_ARCHITECTURE_TESTS_TASK_NAME = "generateArchitectureTests";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("java");

        ArchitectureValidatorExtension extension = project.getExtensions().create(
                ArchitectureValidatorExtension.NAME,
                ArchitectureValidatorExtension.class,
                project.getObjects(),
                project.getLayout());

        Provider<String> defaultBasePackage = project.provider(() -> {
            Object group = project.getGroup();
            return group == null || "unspecified".equals(group.toString()) ? "" : group.toString();
        });
        extension.getBasePackage().convention(defaultBasePackage);

        var generateTask = project.getTasks().register(
                GENERATE_ARCHITECTURE_TESTS_TASK_NAME,
                GenerateArchitectureTestsTask.class,
                task -> {
                    task.getBasePackage().set(extension.getBasePackage());
                    task.getInPorts().set(extension.getHexagonalArchitecture().getInPorts());
                    task.getOutPorts().set(extension.getHexagonalArchitecture().getOutPorts());
                    task.getDomainModel().set(extension.getHexagonalArchitecture().getDomainModel());
                    task.getAdapters().set(extension.getHexagonalArchitecture().getAdapters());
                    task.getApplicationServices().set(extension.getHexagonalArchitecture().getApplicationServices());
                    task.getCommonPackages().set(extension.getHexagonalArchitecture().getCommonPackages());
                    task.getFailOnDuplicateRules().set(extension.getFailOnDuplicateRules());
                    task.getUseBuiltInHexagonalRulePack().set(extension.getUseBuiltInHexagonalRulePack());
                    task.getUserTestsDirectory().set(extension.getTestDirectory());
                    task.getOutputDirectory().set(project.getLayout().getBuildDirectory().dir("generated/testArchitecture/java"));
                });

        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);
        var suiteProvider = testing.getSuites().register(TEST_ARCHITECTURE_TASK_NAME, JvmTestSuite.class, suite -> {
            suite.useJUnitJupiter();
            suite.getSources().getJava().srcDir(extension.getTestDirectory());
            suite.getSources().getJava().srcDir(generateTask.flatMap(GenerateArchitectureTestsTask::getOutputDirectory));
        });

        project.getDependencies().add(TEST_ARCHITECTURE_TASK_NAME + "Implementation", "com.tngtech.archunit:archunit-junit5:1.4.1");
        project.getDependencies().add(TEST_ARCHITECTURE_TASK_NAME + "Implementation", "org.junit.platform:junit-platform-suite-api:6.1.0");
        project.getDependencies().add(TEST_ARCHITECTURE_TASK_NAME + "RuntimeOnly", "org.junit.platform:junit-platform-suite-engine:6.1.0");

        JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        SourceSet mainSourceSet = javaExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        project.getDependencies().add(
            TEST_ARCHITECTURE_TASK_NAME + "Implementation",
            project.getDependencies().create(mainSourceSet.getOutput()));

        generateTask.configure(task -> task.getRulePackClasspath().from(project.getConfigurations().named(TEST_ARCHITECTURE_TASK_NAME + "CompileClasspath")));

        project.getTasks().named("compileTestArchitectureJava").configure(task -> task.dependsOn(generateTask));

        suiteProvider.configure(suite -> suite.getTargets().all(target -> target.getTestTask().configure(testTask -> {
            configureArchitectureTestTask(project, extension, testTask);
        })));

        project.getTasks().named("check").configure(task -> task.dependsOn(TEST_ARCHITECTURE_TASK_NAME));
    }

    private void configureArchitectureTestTask(
            Project project,
            ArchitectureValidatorExtension extension,
            Test testTask
    ) {
        AtomicLong failedTests = new AtomicLong();

        testTask.useJUnitPlatform();
        testTask.setIgnoreFailures(true);
        testTask.systemProperty("architectureValidator.basePackage", extension.getBasePackage().getOrElse(""));
        testTask.systemProperty("architectureValidator.inPorts", String.join(",", extension.getHexagonalArchitecture().getInPorts().get()));
        testTask.systemProperty("architectureValidator.outPorts", String.join(",", extension.getHexagonalArchitecture().getOutPorts().get()));
        testTask.systemProperty("architectureValidator.domainModel", String.join(",", extension.getHexagonalArchitecture().getDomainModel().get()));
        testTask.systemProperty("architectureValidator.adapters", String.join(",", extension.getHexagonalArchitecture().getAdapters().get()));
        testTask.systemProperty("architectureValidator.applicationServices", String.join(",", extension.getHexagonalArchitecture().getApplicationServices().get()));
        testTask.systemProperty("architectureValidator.commonPackages", String.join(",", extension.getHexagonalArchitecture().getCommonPackages().get()));
        testTask.getReports().getHtml().getOutputLocation().set(project.getLayout().getBuildDirectory().dir("reports/architecture-validator/html"));
        testTask.getReports().getJunitXml().getOutputLocation().set(project.getLayout().getBuildDirectory().dir("reports/architecture-validator/xml"));

        testTask.addTestListener(new ArchitectureValidationTestListener(failedTests));

        testTask.doLast(task -> {
            if (extension.getIgnoreFailures().getOrElse(false) || !extension.getFailOnViolation().getOrElse(true)) {
                return;
            }
            long maxAllowedViolations = extension.getMaxAllowedViolations().getOrElse(0);
            if (failedTests.get() > maxAllowedViolations) {
                throw new GradleException(
                        "Architecture validation failed with " + failedTests.get()
                                + " failing rule(s); maxAllowedViolations=" + maxAllowedViolations);
            }
        });
    }

    static final class ArchitectureValidationTestListener implements TestListener {
        private final AtomicLong failedTests;

        ArchitectureValidationTestListener(AtomicLong failedTests) {
            this.failedTests = failedTests;
        }

        @Override
        public void beforeSuite(TestDescriptor suite) {
        }

        @Override
        public void afterSuite(TestDescriptor suite, TestResult result) {
            if (suite.getParent() == null) {
                failedTests.set(result.getFailedTestCount());
            }
        }

        @Override
        public void beforeTest(TestDescriptor testDescriptor) {
        }

        @Override
        public void afterTest(TestDescriptor testDescriptor, TestResult result) {
        }
    }
}