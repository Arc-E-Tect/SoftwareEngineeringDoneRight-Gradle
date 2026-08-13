package com.arc_e_tect.gradle.trackerlens.lens;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merges the {@link Lens}es contributed by several {@link LensSource}s into the final set offered
 * in the dashboard's lens switcher.
 *
 * <p>Within a single source, ids are already unique (enforced defensively by {@link LensScanner}).
 * Across sources - built-in vs. external, or external vs. external, no distinction is made between
 * the two - a colliding id is transparently auto-namespaced: the first source to contribute a given
 * id keeps it clean, and every later source contributing the same id gets it suffixed with its
 * source label in parentheses (e.g. {@code "dark-lens (midnight-theme)"}). This is a no-fail,
 * always-succeeds merge: id collisions are expected and handled, never an error.</p>
 *
 * <p>Sources are merged strictly in the order given, so a caller that wants one particular source
 * to win a contested id outright (e.g. {@code trackerLens.lensStylesheet}, which must always keep
 * its clean {@code custom-lens} id) achieves that simply by placing it first in the list - this
 * class applies no other precedence rule.</p>
 */
public class LensResolver {

    /** Creates a new {@code LensResolver}. */
    public LensResolver() {}

    /**
     * Merges {@code sources} in order, auto-namespacing any id collision across sources.
     *
     * @param sources the contributors to merge, in precedence order
     * @return the resolved lenses, in the order first discovered
     */
    public List<ResolvedLens> resolve(List<LensSource> sources) {
        Map<String, Boolean> seenIds = new LinkedHashMap<>();
        List<ResolvedLens> resolved = new ArrayList<>();
        for (LensSource source : sources) {
            for (Lens lens : source.lenses()) {
                String id = seenIds.containsKey(lens.id())
                        ? lens.id() + " (" + source.label() + ")"
                        : lens.id();
                seenIds.put(lens.id(), true);
                resolved.add(new ResolvedLens(id, source.label(), lens.content()));
            }
        }
        return resolved;
    }
}
