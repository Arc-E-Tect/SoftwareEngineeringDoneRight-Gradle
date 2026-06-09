package com.arc_e_tect.gradle.architecture;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

final class PluginMetadata {

    private static final String RESOURCE_NAME = "architecture-validator.properties";
    private static final String UNKNOWN_VERSION = "0.0.0";

    private PluginMetadata() {
    }

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