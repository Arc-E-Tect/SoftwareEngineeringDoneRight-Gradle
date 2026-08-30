package com.arc_e_tect.example.endpoints;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Reads URI path templates from {@code api-endpoints.properties}, the single source of truth
 * every test suite calling this service's HTTP API draws from. A path-prefix change then touches
 * one file instead of every call site. This mirrors the pattern used by the User Account
 * microservice in the accompanying book's example code - the pattern this Doppelganger example is
 * designed to demonstrate the plugin can now see through.
 */
public final class ApiEndpoints {

    private static final Properties ENDPOINTS = load();

    private ApiEndpoints() {
    }

    public static String get(String key) {
        String value = ENDPOINTS.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("No api-endpoints.properties entry for key '" + key + "'");
        }
        return value;
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = ApiEndpoints.class.getClassLoader().getResourceAsStream("api-endpoints.properties")) {
            if (in == null) {
                throw new IllegalStateException("api-endpoints.properties not found on the classpath");
            }
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return properties;
    }
}
