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
 * {@code META-INF/arc-e-tect/tracker-lens/lenses/*.css} convention and returns the {@link Lens}es
 * it finds.
 *
 * <p>This is the single method called both for this plugin's own bundled built-in lenses and for
 * every resolved external style pack: only the technical means of locating the classpath root
 * {@code File} to pass in differs between the two: it is never special-cased here. See
 * {@link LensResolver} for the cross-root merge, collision, and auto-namespacing logic that follows
 * scanning.</p>
 */
public class LensScanner {

    private static final String LENS_DIR = "META-INF/arc-e-tect/tracker-lens/lenses/";

    /** Creates a new {@code LensScanner}. */
    public LensScanner() {}

    /**
     * Scans {@code classpathRoot} for lenses.
     *
     * @param classpathRoot a directory or a {@code .jar} file; must exist
     * @return the lenses found, in discovery order
     * @throws GradleException if {@code classpathRoot} ships two lens files that resolve to the
     *                          same lower-cased id (a defensive check: within one pack, one
     *                          directory is inherently one namespace, so this should not happen in
     *                          practice)
     */
    public List<Lens> scan(File classpathRoot) {
        try {
            List<Lens> lenses = classpathRoot.isDirectory()
                    ? scanDirectory(classpathRoot)
                    : scanJar(classpathRoot);
            failOnDuplicateIds(classpathRoot, lenses);
            return lenses;
        } catch (IOException e) {
            throw new GradleException("trackerLens: failed to scan for lenses in " + classpathRoot, e);
        }
    }

    private List<Lens> scanDirectory(File root) throws IOException {
        File lensDir = new File(root, LENS_DIR);
        if (!lensDir.isDirectory()) {
            return List.of();
        }
        List<Lens> lenses = new ArrayList<>();
        try (Stream<java.nio.file.Path> paths = Files.list(lensDir.toPath())) {
            for (java.nio.file.Path path : paths.sorted().toList()) {
                String fileName = path.getFileName().toString();
                if (Files.isRegularFile(path) && fileName.endsWith(".css")) {
                    lenses.add(new Lens(idFor(fileName), Files.readAllBytes(path)));
                }
            }
        }
        return lenses;
    }

    private List<Lens> scanJar(File jarFile) throws IOException {
        List<Lens> lenses = new ArrayList<>();
        try (JarFile jar = new JarFile(jarFile)) {
            List<JarEntry> matching = jar.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> entry.getName().startsWith(LENS_DIR))
                    .filter(entry -> entry.getName().endsWith(".css"))
                    .filter(entry -> !entry.getName().substring(LENS_DIR.length()).contains("/"))
                    .sorted((a, b) -> a.getName().compareTo(b.getName()))
                    .toList();
            for (JarEntry entry : matching) {
                String fileName = entry.getName().substring(LENS_DIR.length());
                try (InputStream stream = jar.getInputStream(entry)) {
                    lenses.add(new Lens(idFor(fileName), stream.readAllBytes()));
                }
            }
        }
        return lenses;
    }

    private void failOnDuplicateIds(File classpathRoot, List<Lens> lenses) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Lens lens : lenses) {
            counts.merge(lens.id(), 1, Integer::sum);
        }
        List<String> duplicates = counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
        if (!duplicates.isEmpty()) {
            throw new GradleException("trackerLens: style pack " + classpathRoot
                    + " ships more than one lens file resolving to the same id: " + duplicates);
        }
    }

    private String idFor(String fileName) {
        String baseName = fileName.endsWith(".css") ? fileName.substring(0, fileName.length() - 4) : fileName;
        return baseName.toLowerCase(Locale.ROOT);
    }
}
