package com.arc_e_tect.gradle.trackerlens.lens;

/**
 * A single lens-pack dashboard template, as discovered by {@link TemplateScanner}: its id (the
 * template file's base name, lower-cased) and its Mustache content.
 *
 * <p>Content is read eagerly into memory at scan time rather than kept as a {@code File}, since a
 * template discovered inside a jar has no standalone {@code File} of its own - reading eagerly lets
 * a directory-backed and a jar-backed template be represented identically. Mirrors {@link Lens}
 * exactly; see that type's own javadoc for why.</p>
 *
 * @param id      the template id: the file's base name, lower-cased
 * @param content the raw Mustache template file bytes
 */
public record Template(String id, byte[] content) {
}
