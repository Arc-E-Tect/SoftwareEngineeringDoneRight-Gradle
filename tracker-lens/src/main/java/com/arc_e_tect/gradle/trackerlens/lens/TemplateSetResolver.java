package com.arc_e_tect.gradle.trackerlens.lens;

import org.gradle.api.GradleException;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the full merged template set - this plugin's own bundled built-in template, plus every
 * resolved external {@code lensStyle} pack's templates (optionally restricted to one preferred
 * pack) - exactly the same way regardless of which task asks for it.
 *
 * <p>Deliberately has no {@code trackerLens.template}-equivalent parameter: a project's own one-off
 * template file is a separate, pre-existing mechanism entirely outside lens-pack template discovery
 * - see {@link TemplateSource}'s own javadoc. Shared by {@code GenerateTrackerLensTask} (which
 * looks up one selected id's content to render) and {@code ListTrackerLensTemplatesTask} (which
 * only reports the set) - both call this one method rather than each re-implementing template-source
 * assembly, so the two can never drift apart on what "the available templates" means. Mirrors
 * {@link LensSetResolver} exactly; see that type's own javadoc for the full reasoning.</p>
 */
public class TemplateSetResolver {

    private static final String BUILT_IN_TEMPLATE_MARKER =
            "META-INF/arc-e-tect/tracker-lens/templates/default.html";

    private final TemplateScanner templateScanner = new TemplateScanner();
    private final TemplateResolver templateResolver = new TemplateResolver();

    /** Creates a new {@code TemplateSetResolver}. */
    public TemplateSetResolver() {}

    /**
     * Resolves the full merged template set.
     *
     * @param lensStyleClasspath classpath entries (directories or jars) resolved from the
     *                           {@code lensStyle} configuration; a non-existent entry is skipped
     * @param preferredLensPack  optional {@code group:artifact} coordinate restricting the
     *                           external packs considered to the one whose derived label matches
     *                           its artifact part; blank or {@code null} considers every pack
     * @return the resolved, auto-namespaced template set
     */
    public List<ResolvedTemplate> resolve(Iterable<File> lensStyleClasspath, String preferredLensPack) {
        List<TemplateSource> sources = new ArrayList<>();

        sources.add(new TemplateSource("built-in", templateScanner.scan(resolveBuiltInTemplateRoot())));

        String preferredLabel = (preferredLensPack == null || preferredLensPack.isBlank())
                ? null : PackLabel.labelPart(preferredLensPack);
        for (File classpathEntry : lensStyleClasspath) {
            if (!classpathEntry.exists()) {
                continue;
            }
            String label = PackLabel.labelFor(classpathEntry);
            if (preferredLabel != null && !preferredLabel.equals(label)) {
                continue;
            }
            sources.add(new TemplateSource(label, templateScanner.scan(classpathEntry)));
        }

        return templateResolver.resolve(sources);
    }

    private File resolveBuiltInTemplateRoot() {
        URL resource = getClass().getClassLoader().getResource(BUILT_IN_TEMPLATE_MARKER);
        if (resource == null) {
            throw new GradleException(
                    "trackerLens: missing bundled built-in template resources: " + BUILT_IN_TEMPLATE_MARKER);
        }
        String urlString = resource.toString();
        try {
            if (urlString.startsWith("jar:")) {
                String jarUrl = urlString.substring(4, urlString.indexOf("!/"));
                return new File(URI.create(jarUrl));
            }
            String rootUrl = urlString.substring(0, urlString.length() - BUILT_IN_TEMPLATE_MARKER.length());
            return new File(URI.create(rootUrl));
        } catch (IllegalArgumentException e) {
            throw new GradleException("trackerLens: could not resolve built-in template classpath root from "
                    + urlString, e);
        }
    }
}
