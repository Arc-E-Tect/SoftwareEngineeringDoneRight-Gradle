package com.arc_e_tect.gradle.trackerlens.lens;

/**
 * A single style pack lens, as discovered by {@link LensScanner}: its id (the lens file's base
 * name, lower-cased) and its CSS content.
 *
 * <p>Content is read eagerly into memory at scan time rather than kept as a {@code File}, since a
 * lens discovered inside a jar has no standalone {@code File} of its own - reading eagerly lets a
 * directory-backed and a jar-backed lens be represented identically.</p>
 *
 * @param id      the lens id: the CSS file's base name, lower-cased
 * @param content the raw CSS file bytes
 */
public record Lens(String id, byte[] content) {
}
