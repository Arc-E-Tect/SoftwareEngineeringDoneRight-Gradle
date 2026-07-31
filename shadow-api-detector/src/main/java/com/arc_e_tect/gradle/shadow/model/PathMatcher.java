package com.arc_e_tect.gradle.shadow.model;

/**
 * Compares two normalised path templates for structural equivalence, tolerant of path-variable
 * naming differences (e.g. Spring's {@code "{id}"} against an OpenAPI {@code "{userId}"}).
 */
public final class PathMatcher {

    private PathMatcher() {}

    /**
     * Returns whether two normalised path templates are structurally equivalent: same number of
     * {@code "/"}-delimited segments, with every literal segment equal and every placeholder
     * segment (e.g. {@code "{id}"}) aligned with a placeholder segment on the other side,
     * regardless of the placeholder's variable name.
     *
     * @param pathA a path template normalised via {@link PathTemplates#normalize(String)}
     * @param pathB a path template normalised via {@link PathTemplates#normalize(String)}
     * @return {@code true} when the two templates describe the same set of concrete paths
     */
    public static boolean matches(String pathA, String pathB) {
        String[] segmentsA = segments(pathA);
        String[] segmentsB = segments(pathB);
        if (segmentsA.length != segmentsB.length) {
            return false;
        }
        for (int i = 0; i < segmentsA.length; i++) {
            String a = segmentsA[i];
            String b = segmentsB[i];
            boolean aPlaceholder = PathTemplates.isPlaceholder(a);
            boolean bPlaceholder = PathTemplates.isPlaceholder(b);
            if (aPlaceholder != bPlaceholder) {
                return false;
            }
            if (!aPlaceholder && !a.equals(b)) {
                return false;
            }
        }
        return true;
    }

    private static String[] segments(String path) {
        if ("/".equals(path)) {
            return new String[0];
        }
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        return trimmed.split("/");
    }
}
