package com.arc_e_tect.gradle.trackerlens.lens;

import org.gradle.api.GradleException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Scans one classpath root - a directory or a jar, this plugin's own bundled resources or a
 * resolved {@code lensStyle} dependency, no distinction - for the fixed
 * {@code META-INF/arc-e-tect/tracker-lens/templates/*.html} convention and returns the
 * {@link Template}s it finds.
 *
 * <p>A lens pack is not just a color scheme - it can also ship one or more Mustache dashboard
 * templates, each a whole alternate view over the same tracking data, selectable via
 * {@code trackerLens.templateId}. This mirrors {@link LensScanner} exactly, one directory over from
 * {@code lenses/}; see that type's own javadoc for the reasoning behind every design choice here,
 * none of which differs for templates.</p>
 */
public class TemplateScanner {

    private static final String TEMPLATE_DIR = "META-INF/arc-e-tect/tracker-lens/templates/";

    /** Creates a new {@code TemplateScanner}. */
    public TemplateScanner() {}

    /**
     * Scans {@code classpathRoot} for templates.
     *
     * @param classpathRoot a directory or a {@code .jar} file; must exist
     * @return the templates found, in discovery order
     * @throws GradleException if {@code classpathRoot} ships two template files that resolve to
     *                          the same lower-cased id
     */
    public List<Template> scan(File classpathRoot) {
        try {
            List<Template> templates = classpathRoot.isDirectory()
                    ? scanDirectory(classpathRoot)
                    : scanJar(classpathRoot);
            failOnDuplicateIds(classpathRoot, templates);
            return templates;
        } catch (IOException e) {
            throw new GradleException("trackerLens: failed to scan for templates in " + classpathRoot, e);
        }
    }

    private List<Template> scanDirectory(File root) throws IOException {
        File templateDir = new File(root, TEMPLATE_DIR);
        if (!templateDir.isDirectory()) {
            return List.of();
        }
        List<Template> templates = new ArrayList<>();
        try (Stream<java.nio.file.Path> paths = Files.list(templateDir.toPath())) {
            for (java.nio.file.Path path : paths.sorted().toList()) {
                String fileName = path.getFileName().toString();
                if (Files.isRegularFile(path) && fileName.endsWith(".html")) {
                    templates.add(new Template(idFor(fileName), Files.readAllBytes(path)));
                }
            }
        }
        return templates;
    }

    private List<Template> scanJar(File jarFile) throws IOException {
        List<Template> templates = new ArrayList<>();
        try (JarFile jar = new JarFile(jarFile)) {
            List<JarEntry> matching = jar.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> entry.getName().startsWith(TEMPLATE_DIR))
                    .filter(entry -> entry.getName().endsWith(".html"))
                    .filter(entry -> !entry.getName().substring(TEMPLATE_DIR.length()).contains("/"))
                    .sorted((a, b) -> a.getName().compareTo(b.getName()))
                    .toList();
            for (JarEntry entry : matching) {
                String fileName = entry.getName().substring(TEMPLATE_DIR.length());
                try (InputStream stream = jar.getInputStream(entry)) {
                    templates.add(new Template(idFor(fileName), stream.readAllBytes()));
                }
            }
        }
        return templates;
    }

    private void failOnDuplicateIds(File classpathRoot, List<Template> templates) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Template template : templates) {
            counts.merge(template.id(), 1, Integer::sum);
        }
        List<String> duplicates = counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
        if (!duplicates.isEmpty()) {
            throw new GradleException("trackerLens: style pack " + classpathRoot
                    + " ships more than one template file resolving to the same id: " + duplicates);
        }
    }

    private String idFor(String fileName) {
        String baseName = fileName.endsWith(".html") ? fileName.substring(0, fileName.length() - 5) : fileName;
        return baseName.toLowerCase(Locale.ROOT);
    }
}
