package com.arc_e_tect.gradle.suite;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;

/**
 * DSL extension for the Arc-E-Tect API-Only Suite Gradle plugin.
 *
 * <p>This is a thin convenience extension, not a full proxy for the three underlying detector
 * plugins: it exposes only the two settings that are almost always identical across all three in
 * practice - {@link #getRootDocument()} and {@link #getControllerDirs()}. Any setting not exposed
 * here must be configured directly on the individual plugin's own extension block
 * ({@code shadowApiDetector { }}, {@code mirageApiDetector { }}, {@code doppelgangerApiDetector { }}).</p>
 *
 * <p>Both properties configured here are forwarded to all three underlying extensions as a
 * fallback convention, applied only where a consumer has not already configured that property
 * directly on the individual extension. An explicit per-plugin value always wins over a value
 * configured here, regardless of the order the two are configured in the build script.</p>
 *
 * <pre>
 * apiOnlySuite {
 *     rootDocument = file('src/main/resources/openapi/openapi.yaml')
 *     controllerDirs.from('src/main/java')
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
}
