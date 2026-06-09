package com.arc_e_tect.gradle.architecture;

import org.gradle.api.Action;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public class ArchitectureValidatorExtension {

    public static final String NAME = "architectureValidator";

    private final DirectoryProperty testDirectory;
    private final Property<String> basePackage;
    private final Property<Boolean> failOnViolation;
    private final Property<Integer> maxAllowedViolations;
    private final Property<Boolean> ignoreFailures;
    private final Property<Boolean> failOnDuplicateRules;
    private final Property<Boolean> useBuiltInHexagonalRulePack;
    private final Property<Boolean> useSpringRulePack;
    private final Property<String> springRulePackCoordinate;
    private final HexagonalArchitectureExtension hexagonalArchitecture;

    @Inject
    public ArchitectureValidatorExtension(ObjectFactory objects, ProjectLayout layout) {
        testDirectory = objects.directoryProperty().convention(layout.getProjectDirectory().dir("src/testArchitecture/java"));
        basePackage = objects.property(String.class).convention("");
        failOnViolation = objects.property(Boolean.class).convention(true);
        maxAllowedViolations = objects.property(Integer.class).convention(0);
        ignoreFailures = objects.property(Boolean.class).convention(false);
        failOnDuplicateRules = objects.property(Boolean.class).convention(false);
        useBuiltInHexagonalRulePack = objects.property(Boolean.class).convention(true);
        useSpringRulePack = objects.property(Boolean.class).convention(false);
        springRulePackCoordinate = objects.property(String.class)
                .convention("com.arc-e-tect:architecture-validator-spring-rules");
        hexagonalArchitecture = objects.newInstance(HexagonalArchitectureExtension.class);
    }

    public DirectoryProperty getTestDirectory() {
        return testDirectory;
    }

    public Property<String> getBasePackage() {
        return basePackage;
    }

    public Property<Boolean> getFailOnViolation() {
        return failOnViolation;
    }

    public Property<Integer> getMaxAllowedViolations() {
        return maxAllowedViolations;
    }

    public Property<Boolean> getIgnoreFailures() {
        return ignoreFailures;
    }

    public Property<Boolean> getFailOnDuplicateRules() {
        return failOnDuplicateRules;
    }

    public Property<Boolean> getUseBuiltInHexagonalRulePack() {
        return useBuiltInHexagonalRulePack;
    }

    public Property<Boolean> getUseSpringRulePack() {
        return useSpringRulePack;
    }

    public Property<String> getSpringRulePackCoordinate() {
        return springRulePackCoordinate;
    }

    public HexagonalArchitectureExtension getHexagonalArchitecture() {
        return hexagonalArchitecture;
    }

    public void hexagonalArchitecture(Action<? super HexagonalArchitectureExtension> action) {
        action.execute(hexagonalArchitecture);
    }
}