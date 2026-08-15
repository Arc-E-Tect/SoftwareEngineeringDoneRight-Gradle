package com.arc_e_tect.gradle.trackerlens.lens;

import org.gradle.api.GradleException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the full merged lens set - this plugin's own bundled built-in lenses, every resolved
 * external {@code lensStyle} pack (optionally restricted to one preferred pack), and the single
 * {@code lensStylesheet} lens when configured - exactly the same way regardless of which task asks
 * for it.
 *
 * <p>Shared by {@code GenerateTrackerLensTask} (which renders the resolved set into the dashboard)
 * and {@code ListTrackerLensStylesTask} (which only reports it) - both call this one method rather
 * than each re-implementing lens-source assembly, so the two can never drift apart on what "the
 * available lenses" means.</p>
 */
public class LensSetResolver {

    private static final String BUILT_IN_LENS_MARKER = "META-INF/arc-e-tect/tracker-lens/lenses/light-lens.css";

    private final LensScanner lensScanner = new LensScanner();
    private final LensResolver lensResolver = new LensResolver();

    /** Creates a new {@code LensSetResolver}. */
    public LensSetResolver() {}

    /**
     * Resolves the full merged lens set.
     *
     * @param lensStylesheet     optional single CSS file contributing the {@code custom-lens}
     *                           lens with the highest precedence for that id, or {@code null}
     * @param lensStyleClasspath classpath entries (directories or jars) resolved from the
     *                           {@code lensStyle} configuration; a non-existent entry is skipped
     * @param preferredLensPack  optional {@code group:artifact} coordinate restricting the
     *                           external packs considered to the one whose derived label matches
     *                           its artifact part; blank or {@code null} considers every pack
     * @return the resolved, auto-namespaced lens set
     */
    public List<ResolvedLens> resolve(File lensStylesheet, Iterable<File> lensStyleClasspath, String preferredLensPack) {
        List<LensSource> sources = new ArrayList<>();

        if (lensStylesheet != null) {
            try {
                sources.add(new LensSource("custom",
                        List.of(new Lens("custom-lens", Files.readAllBytes(lensStylesheet.toPath())))));
            } catch (IOException e) {
                throw new GradleException("trackerLens: failed to read lensStylesheet: " + lensStylesheet, e);
            }
        }

        sources.add(new LensSource("built-in", lensScanner.scan(resolveBuiltInLensRoot())));

        String preferredLabel = (preferredLensPack == null || preferredLensPack.isBlank())
                ? null : labelPart(preferredLensPack);
        for (File classpathEntry : lensStyleClasspath) {
            if (!classpathEntry.exists()) {
                continue;
            }
            String label = labelFor(classpathEntry);
            if (preferredLabel != null && !preferredLabel.equals(label)) {
                continue;
            }
            sources.add(new LensSource(label, lensScanner.scan(classpathEntry)));
        }

        return lensResolver.resolve(sources);
    }

    private File resolveBuiltInLensRoot() {
        URL resource = getClass().getClassLoader().getResource(BUILT_IN_LENS_MARKER);
        if (resource == null) {
            throw new GradleException("trackerLens: missing bundled built-in lens resources: " + BUILT_IN_LENS_MARKER);
        }
        String urlString = resource.toString();
        try {
            if (urlString.startsWith("jar:")) {
                String jarUrl = urlString.substring(4, urlString.indexOf("!/"));
                return new File(URI.create(jarUrl));
            }
            String rootUrl = urlString.substring(0, urlString.length() - BUILT_IN_LENS_MARKER.length());
            return new File(URI.create(rootUrl));
        } catch (IllegalArgumentException e) {
            throw new GradleException("trackerLens: could not resolve built-in lens classpath root from " + urlString, e);
        }
    }

    String labelFor(File classpathRoot) {
        return PackLabel.labelFor(classpathRoot);
    }

    String labelPart(String coordinate) {
        return PackLabel.labelPart(coordinate);
    }
}
