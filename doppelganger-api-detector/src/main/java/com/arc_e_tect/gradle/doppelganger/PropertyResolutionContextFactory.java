package com.arc_e_tect.gradle.doppelganger;

import com.arc_e_tect.gradle.detector.core.scan.PropertyResolutionContext;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Builds a {@link PropertyResolutionContext} from the {@code propertyFiles} and
 * {@code pathResolverHelperMethods} configured on {@link DoppelgangerApiDetectorExtension} - shared by
 * {@link DetectDoppelgangerApisTask} and {@link ScanContractsTask}, both of which scan test sources with
 * {@code RestDocsScanner}/{@code OpenApiRequestValidatorScanner} and need the same merged property map
 * and helper-method conventions passed to those scanners. Deliberately package-private and stateless.
 *
 * <p>Each configured file is loaded in one of two ways, chosen by file extension:</p>
 * <ul>
 *     <li>{@code .properties} - loaded with {@link Properties#load(InputStream)}, keys used as-is.</li>
 *     <li>{@code .yml}/{@code .yaml} - parsed with SnakeYAML and flattened to dotted keys, e.g. a
 *     {@code users: { registrations: /api/users }} document contributes the key
 *     {@code users.registrations}.</li>
 * </ul>
 *
 * <p>Files are merged in the order given by {@code propertyFiles}; a key defined in more than one file
 * takes the value from whichever file is iterated last. A configured file that doesn't exist yet is
 * silently skipped (the same "not a build failure" stance the other bootstrapping-gap directories in
 * this plugin take - see {@link ContractScanSupport}) rather than failing the run outright.</p>
 */
final class PropertyResolutionContextFactory {

    private PropertyResolutionContextFactory() {}

    /**
     * Builds a {@link PropertyResolutionContext} from the given configured property files and
     * helper-method conventions.
     *
     * @param propertyFiles              configured {@code .properties}/{@code .yml}/{@code .yaml} files;
     *                                    may be empty
     * @param pathResolverHelperMethods   configured {@code "ClassName.methodName"} helper-method
     *                                    conventions; may be empty
     * @return a context merging every existing configured file's contents, or
     *         {@link PropertyResolutionContext#empty()} when nothing is configured
     */
    static PropertyResolutionContext create(
            ConfigurableFileCollection propertyFiles, ListProperty<String> pathResolverHelperMethods) {
        Map<String, String> merged = new LinkedHashMap<>();
        for (File file : propertyFiles) {
            if (!file.isFile()) {
                continue;
            }
            String name = file.getName().toLowerCase();
            try {
                if (name.endsWith(".yml") || name.endsWith(".yaml")) {
                    loadYaml(file, merged);
                } else {
                    loadProperties(file, merged);
                }
            } catch (IOException e) {
                throw new org.gradle.api.GradleException(
                        "Could not read configured property file `" + file + "`: " + e.getMessage(), e);
            }
        }
        Set<String> helperMethods = new LinkedHashSet<>(pathResolverHelperMethods.getOrElse(List.of()));
        if (merged.isEmpty() && helperMethods.isEmpty()) {
            return PropertyResolutionContext.empty();
        }
        return PropertyResolutionContext.of(merged, helperMethods);
    }

    private static void loadProperties(File file, Map<String, String> target) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            properties.load(in);
        } catch (FileNotFoundException e) {
            // Configured file disappeared between the isFile() check and here (e.g. concurrent
            // build); treat the same as "doesn't exist yet" rather than failing the run.
            return;
        }
        for (String key : properties.stringPropertyNames()) {
            target.put(key, properties.getProperty(key));
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadYaml(File file, Map<String, String> target) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            Object loaded = new Yaml().load(in);
            if (loaded instanceof Map<?, ?> map) {
                flatten("", (Map<String, Object>) map, target);
            }
        } catch (FileNotFoundException e) {
            // Same rationale as loadProperties: treat a file removed after the isFile() check as
            // "doesn't exist yet".
        }
    }

    @SuppressWarnings("unchecked")
    private static void flatten(String prefix, Map<String, Object> map, Map<String, String> target) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                flatten(key, (Map<String, Object>) nested, target);
            } else if (value != null) {
                target.put(key, String.valueOf(value));
            }
        }
    }
}
