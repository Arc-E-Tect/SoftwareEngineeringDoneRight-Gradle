package com.arc_e_tect.gradle.trackerlens;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads this plugin's own version from the {@code tracker-lens.properties} classpath resource,
 * stamped with the real build version by {@code processResources} at build time (the same version
 * the release workflow computes and writes into {@code gradle.properties} before building).
 *
 * <p>Used by {@code initTrackerLens} and {@code bootstrapTrackerLensProject} to generate boilerplate
 * that always pins the plugin version currently in use, rather than a hand-maintained, driftable
 * placeholder - regenerate the boilerplate after upgrading the plugin and it picks up the new
 * version automatically.</p>
 */
final class PluginMetadata {

    private static final String RESOURCE_NAME = "tracker-lens.properties";
    private static final String UNKNOWN_VERSION = "0.0.0";

    private PluginMetadata() {}

    static String pluginVersion() {
        Properties properties = new Properties();
        try (InputStream stream = PluginMetadata.class.getClassLoader().getResourceAsStream(RESOURCE_NAME)) {
            if (stream == null) {
                return UNKNOWN_VERSION;
            }
            properties.load(stream);
            return properties.getProperty("pluginVersion", UNKNOWN_VERSION);
        } catch (IOException ignored) {
            return UNKNOWN_VERSION;
        }
    }
}
