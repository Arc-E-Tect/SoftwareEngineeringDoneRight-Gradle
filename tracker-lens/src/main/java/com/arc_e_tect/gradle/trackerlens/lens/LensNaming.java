package com.arc_e_tect.gradle.trackerlens.lens;

import java.util.Locale;

/**
 * Derives a filesystem-safe CSS file name from a (possibly auto-namespaced) lens id, e.g.
 * {@code "dark-lens (midnight-theme)"} becomes {@code "dark-lens-midnight-theme.css"}.
 *
 * <p>Used consistently wherever a resolved lens's id needs to become a file name - when
 * {@code GenerateTrackerLensTask} writes each lens's CSS file, and when the dashboard's
 * {@code <link>}/{@code <option>} markup references it - so the two always agree.</p>
 */
public final class LensNaming {

    private LensNaming() {}

    /**
     * Derives the CSS file name for {@code lensId}.
     *
     * @param lensId a resolved lens id
     * @return a filesystem-safe file name ending in {@code .css}
     */
    public static String cssFileName(String lensId) {
        String slug = lensId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+", "-").replaceAll("-{2,}", "-");
        slug = slug.replaceAll("^-|-$", "");
        return slug + ".css";
    }
}
