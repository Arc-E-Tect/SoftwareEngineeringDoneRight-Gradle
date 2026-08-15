package com.arc_e_tect.gradle.trackerlens.lens;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merges the {@link Template}s contributed by several {@link TemplateSource}s into the final set
 * {@code listTrackerLensTemplates} reports and {@code trackerLens.templateId} selects from.
 *
 * <p>Mirrors {@link LensResolver} exactly: within a single source, ids are already unique (enforced
 * defensively by {@link TemplateScanner}); across sources, a colliding id is auto-namespaced with
 * its source label, first-listed source wins the clean id. See {@link LensResolver}'s own javadoc
 * for the full reasoning, none of which differs for templates.</p>
 */
public class TemplateResolver {

    /** Creates a new {@code TemplateResolver}. */
    public TemplateResolver() {}

    /**
     * Merges {@code sources} in order, auto-namespacing any id collision across sources.
     *
     * @param sources the contributors to merge, in precedence order
     * @return the resolved templates, in the order first discovered
     */
    public List<ResolvedTemplate> resolve(List<TemplateSource> sources) {
        Map<String, Boolean> seenIds = new LinkedHashMap<>();
        List<ResolvedTemplate> resolved = new ArrayList<>();
        for (TemplateSource source : sources) {
            for (Template template : source.templates()) {
                String id = seenIds.containsKey(template.id())
                        ? template.id() + " (" + source.label() + ")"
                        : template.id();
                seenIds.put(template.id(), true);
                resolved.add(new ResolvedTemplate(id, source.label(), template.content()));
            }
        }
        return resolved;
    }
}
