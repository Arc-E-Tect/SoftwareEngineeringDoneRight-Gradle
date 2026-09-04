package com.arc_e_tect.gradle.architecture;

import org.gradle.api.Action;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.util.List;

/**
 * DSL extension for the Architecture Validator Gradle plugin.
 *
 * <pre>
 * architectureValidator {
 *     basePackage = 'com.example.myapp'
 *     failOnViolation = true
 *
 *     hexagonalArchitecture {
 *         inPorts = ['..application.port.inbound..']
 *     }
 * }
 * </pre>
 */
public class ArchitectureValidatorExtension {

    /** Extension DSL block name, i.e. the name used to register the extension with the project. */
    public static final String NAME = "architectureValidator";

    private final DirectoryProperty testDirectory;
    private final Property<String> basePackage;
    private final Property<Boolean> failOnViolation;
    private final Property<Integer> maxAllowedViolations;
    private final Property<Boolean> ignoreFailures;
    private final Property<Boolean> failOnDuplicateRules;
    private final Property<Boolean> useBuiltInHexagonalRulePack;
    private final Property<String> junitVersion;
    private final ListProperty<String> rulesDisabled;
    private final HexagonalArchitectureExtension hexagonalArchitecture;

    /**
     * Creates the extension, with every property already set to its default. Instantiated by
     * Gradle's extension-creation infrastructure.
     *
     * @param objects Gradle's object factory
     * @param layout  the project's layout, used to default {@link #getTestDirectory()}
     */
    @Inject
    public ArchitectureValidatorExtension(ObjectFactory objects, ProjectLayout layout) {
        testDirectory = objects.directoryProperty().convention(layout.getProjectDirectory().dir("src/testArchitecture/java"));
        basePackage = objects.property(String.class).convention("");
        failOnViolation = objects.property(Boolean.class).convention(true);
        maxAllowedViolations = objects.property(Integer.class).convention(0);
        ignoreFailures = objects.property(Boolean.class).convention(false);
        failOnDuplicateRules = objects.property(Boolean.class).convention(false);
        useBuiltInHexagonalRulePack = objects.property(Boolean.class).convention(true);
        junitVersion = objects.property(String.class).convention("6.1.0");
        rulesDisabled = objects.listProperty(String.class).convention(List.of());
        hexagonalArchitecture = objects.newInstance(HexagonalArchitectureExtension.class);
    }

    /**
     * Directory holding hand-written architecture tests, compiled into the {@code testArchitecture}
     * suite alongside the generated ones.
     *
     * @return mutable directory property for the hand-written test directory
     */
    public DirectoryProperty getTestDirectory() {
        return testDirectory;
    }

    /**
     * Root package ArchUnit rules are scoped to. Defaults to the empty string; the plugin further
     * defaults an unset value to the project's own {@code group} at apply time.
     *
     * @return mutable property for the base package
     */
    public Property<String> getBasePackage() {
        return basePackage;
    }

    /**
     * Whether the build fails when a rule is violated.
     *
     * @return mutable property for the fail-on-violation flag
     */
    public Property<Boolean> getFailOnViolation() {
        return failOnViolation;
    }

    /**
     * Violation count tolerated before failing, when {@link #getFailOnViolation()} is {@code true}.
     *
     * @return mutable property for the allowed violation count
     */
    public Property<Integer> getMaxAllowedViolations() {
        return maxAllowedViolations;
    }

    /**
     * Whether violations are reported but never fail the build, overriding
     * {@link #getFailOnViolation()}/{@link #getMaxAllowedViolations()}.
     *
     * @return mutable property for the ignore-failures flag
     */
    public Property<Boolean> getIgnoreFailures() {
        return ignoreFailures;
    }

    /**
     * Whether the build fails when duplicate rules are discovered across the built-in rule pack,
     * external rule packs, and hand-written tests.
     *
     * @return mutable property for the fail-on-duplicate-rules flag
     */
    public Property<Boolean> getFailOnDuplicateRules() {
        return failOnDuplicateRules;
    }

    /**
     * Whether the built-in hexagonal rule pack is generated, in addition to any external rule packs
     * and hand-written tests.
     *
     * @return mutable property for the use-built-in-hexagonal-rule-pack flag
     */
    public Property<Boolean> getUseBuiltInHexagonalRulePack() {
        return useBuiltInHexagonalRulePack;
    }

    /**
     * JUnit version used for the {@code testArchitecture} suite. The plugin maps JUnit Platform
     * Suite dependencies to the matching line automatically.
     *
     * @return mutable property for the JUnit version
     */
    public Property<String> getJunitVersion() {
        return junitVersion;
    }

    /**
     * Convenience setter for {@link #getJunitVersion()}, for a build script pulling the version
     * from a version catalog (e.g. {@code setJunitVersion(libs.versions.junit.get())}).
     *
     * @param junitVersion the value to set {@link #getJunitVersion()} to
     */
    public void setJunitVersion(String junitVersion) {
        this.junitVersion.set(junitVersion);
    }

    /**
     * Rule names to skip, whether from the built-in hexagonal rule pack, an external rule pack, or
     * a hand-written test.
     *
     * @return mutable list property of disabled rule names
     */
    public ListProperty<String> getRulesDisabled() {
        return rulesDisabled;
    }

    /**
     * Package-pattern overrides for the built-in hexagonal rule pack.
     *
     * @return the nested hexagonal-architecture configuration block
     */
    public HexagonalArchitectureExtension getHexagonalArchitecture() {
        return hexagonalArchitecture;
    }

    /**
     * Configures {@link #getHexagonalArchitecture()}.
     *
     * @param action the configuration block
     */
    public void hexagonalArchitecture(Action<? super HexagonalArchitectureExtension> action) {
        action.execute(hexagonalArchitecture);
    }
}
