package com.arc_e_tect.gradle.gherkin.snippet;

/** Converts a {@code Feature} title into a directory-safe camelCase name. */
public final class FeatureNameFormatter {

    private FeatureNameFormatter() {}

    /**
     * Converts {@code featureTitle} into camelCase with no spaces, e.g.
     * {@code "User authentication"} becomes {@code "userAuthentication"}.
     *
     * @param featureTitle the Feature's title, as parsed from the {@code .feature} file
     * @return the camelCase directory name; empty if {@code featureTitle} is blank
     */
    public static String toDirectoryName(String featureTitle) {
        String[] words = featureTitle.trim().split("\\s+");
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (name.isEmpty()) {
                name.append(Character.toLowerCase(word.charAt(0))).append(word.substring(1));
            } else {
                name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return name.toString();
    }
}
