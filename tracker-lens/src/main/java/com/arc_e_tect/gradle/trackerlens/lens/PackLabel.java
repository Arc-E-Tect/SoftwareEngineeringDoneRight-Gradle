package com.arc_e_tect.gradle.trackerlens.lens;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Derives a {@code lensStyle} pack's auto-namespacing label from its resolved classpath entry, and
 * the artifact-name half of a {@code group:artifact} coordinate - shared by {@link LensSetResolver}
 * and {@link TemplateSetResolver}, since a pack's label means the same thing regardless of which
 * kind of resource (CSS lens or Mustache template) is being namespaced by it.
 */
final class PackLabel {

    private static final Pattern TRAILING_VERSION = Pattern.compile("^(.*)-\\d[\\w.\\-]*$");

    private PackLabel() {}

    /**
     * Derives the auto-namespacing label for a resolved classpath entry: its file name, minus a
     * trailing {@code .jar} extension and a trailing version suffix if present.
     *
     * @param classpathRoot a resolved {@code lensStyle} classpath entry (directory or jar)
     * @return the derived label
     */
    static String labelFor(File classpathRoot) {
        String name = classpathRoot.getName();
        if (name.toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) {
            name = name.substring(0, name.length() - 4);
        }
        Matcher matcher = TRAILING_VERSION.matcher(name);
        return matcher.matches() ? matcher.group(1) : name;
    }

    /**
     * The artifact-name half of a {@code group:artifact} coordinate, or the whole string if it
     * carries no {@code group:} prefix.
     *
     * @param coordinate a {@code preferredLensPack}-style coordinate
     * @return the derived label part
     */
    static String labelPart(String coordinate) {
        int colon = coordinate.lastIndexOf(':');
        return colon >= 0 ? coordinate.substring(colon + 1) : coordinate;
    }
}
