package com.arc_e_tect.gradle.trackerlens.lens;

import java.util.List;

/**
 * One contributor of {@link Lens}es to be merged by {@link LensResolver}: either this plugin's own
 * bundled built-in lenses, or one resolved {@code lensStyle} dependency's lenses, or the single lens
 * contributed by {@code trackerLens.lensStylesheet}.
 *
 * <p>{@code label} identifies the contributor for the auto-namespacing suffix applied to a
 * colliding lens id (e.g. {@code "dark-lens (midnight-theme)"}) - it carries no other meaning, and
 * {@link LensResolver} treats every {@code LensSource} identically regardless of what kind of
 * contributor produced it.</p>
 *
 * @param label  identifies this contributor for the auto-namespacing suffix
 * @param lenses the lenses this contributor provides
 */
public record LensSource(String label, List<Lens> lenses) {
}
