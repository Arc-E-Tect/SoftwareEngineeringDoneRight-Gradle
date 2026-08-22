package com.arc_e_tect.gradle.suite;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

/**
 * DSL extension for the Arc-E-Tect API-Only Suite Gradle plugin.
 *
 * <p>This is a thin convenience extension, not a full proxy for the three underlying detector
 * plugins: it exposes only the settings that are almost always identical across all three in
 * practice - {@link #getRootDocument()}, {@link #getControllerDirs()}, and
 * {@link #getFailOnDetection()}. Any setting not exposed here must be configured directly on the
 * individual plugin's own extension block ({@code shadowApiDetector { }},
 * {@code mirageApiDetector { }}, {@code doppelgangerApiDetector { }}).</p>
 *
 * <p>Every property configured here is forwarded to all three underlying extensions as a fallback
 * convention, applied only where a consumer has not already configured that property directly on
 * the individual extension. An explicit per-plugin value always wins over a value configured here,
 * regardless of the order the two are configured in the build script.</p>
 *
 * <pre>
 * apiOnlySuite {
 *     rootDocument = file('src/main/resources/openapi/openapi.yaml')
 *     controllerDirs.from('src/main/java')
 *     failOnDetection = true
 * }
 * </pre>
 */
public abstract class ApiOnlySuiteExtension {

    /** For use by the Gradle-generated concrete subclass. */
    public ApiOnlySuiteExtension() {}

    /** Extension DSL block name, i.e. the name used to register the extension with the project. */
    public static final String NAME = "apiOnlySuite";

    /**
     * The root OpenAPI document shared by all three underlying detector plugins. Forwarded as a
     * fallback convention to each plugin's own {@code rootDocument} property; a value set directly
     * on an individual plugin's own extension always takes precedence over this one.
     *
     * @return mutable file property for the shared root OpenAPI document
     */
    public abstract RegularFileProperty getRootDocument();

    /**
     * Directories to search recursively for {@code @RestController} classes, shared by all three
     * underlying detector plugins (Doppelganger API Detector's separate {@code testDirs} is not
     * covered here, since it has no equivalent in the other two plugins). Forwarded as a fallback
     * to each plugin's own {@code controllerDirs} property; directories configured directly on an
     * individual plugin's own extension always take precedence over this one.
     *
     * @return mutable file collection of shared controller source directories
     */
    public abstract ConfigurableFileCollection getControllerDirs();

    /**
     * Convenience switch that, when {@code true}, forwards as a fallback convention to all three
     * underlying plugins' own fail-on-gap property - {@code shadowApiDetector.failOnShadow},
     * {@code mirageApiDetector.failOnMirage}, and {@code doppelgangerApiDetector.failOnDoppelganger}
     * - so a consumer who wants every detector to fail the build on a genuine finding doesn't need
     * to repeat that intent three times. Defaults to {@code false}, matching every individual
     * plugin's own default.
     *
     * <p>A fail-on-gap value set directly on an individual plugin's own extension always takes
     * precedence over this one, regardless of the order the two are configured in the build script
     * - exactly like {@link #getRootDocument()} and {@link #getControllerDirs()}. This property has
     * no effect on {@code detectAllApiGaps} itself: that aggregate task's own dedicated task
     * instances always force their fail-on-gap property to {@code false}, by design, regardless of
     * this setting or any individual plugin's own configuration - see
     * {@code ApiOnlySuitePlugin}'s class-level documentation.</p>
     *
     * @return mutable boolean property controlling the fallback fail-on-gap value for all three
     *         underlying plugins
     */
    public abstract Property<Boolean> getFailOnDetection();
}
