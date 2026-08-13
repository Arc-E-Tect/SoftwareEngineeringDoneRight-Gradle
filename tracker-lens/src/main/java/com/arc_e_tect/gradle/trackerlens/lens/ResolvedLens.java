package com.arc_e_tect.gradle.trackerlens.lens;

/**
 * A {@link Lens} after cross-pack merging by {@link LensResolver}: its final, possibly
 * auto-namespaced id, the label of the source it came from, and its CSS content.
 *
 * @param id         the final lens id offered in the dashboard's lens switcher
 * @param sourceLabel the label of the {@link LensSource} this lens was resolved from
 * @param content    the raw CSS file bytes
 */
public record ResolvedLens(String id, String sourceLabel, byte[] content) {
}
