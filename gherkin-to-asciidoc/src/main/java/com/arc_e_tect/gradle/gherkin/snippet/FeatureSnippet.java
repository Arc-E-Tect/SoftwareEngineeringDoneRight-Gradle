package com.arc_e_tect.gradle.gherkin.snippet;

import java.io.File;

/**
 * A single snippet file written for one {@code Feature}, within one status.
 *
 * @param featureTitle the Feature's title, e.g. {@code "User authentication"}
 * @param file         the snippet file written for this feature
 */
public record FeatureSnippet(String featureTitle, File file) {
}
