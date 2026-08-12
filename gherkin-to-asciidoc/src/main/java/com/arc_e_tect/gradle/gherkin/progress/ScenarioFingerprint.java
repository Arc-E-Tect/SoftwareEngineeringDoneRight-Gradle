package com.arc_e_tect.gradle.gherkin.progress;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Computes a stable identifier for a scenario from its name alone, deliberately ignoring which
 * {@code Feature} file it currently lives in - so {@link ProgressHistoryUpdater} keeps recognising
 * the same scenario even after it has been moved between feature files.
 *
 * <p>The scenario's {@code Scenario:}/{@code Scenario Outline:} keyword prefix and any existing
 * index prefix (the {@code \d+(?:\.\d+)? - } format {@link com.arc_e_tect.gradle.gherkin.indexing.FeatureIndexer}
 * writes) are both stripped before hashing, so neither the scenario type nor its current numbering
 * affects the fingerprint.</p>
 */
public class ScenarioFingerprint {

    private static final Pattern KEYWORD_PREFIX = Pattern.compile("^(?:Scenario Outline|Scenario):\\s*(.*)$");
    private static final Pattern INDEX_PREFIX = Pattern.compile("^\\d+(?:\\.\\d+)? - (.*)$");
    private static final int FINGERPRINT_LENGTH = 16;

    /** Creates a new {@code ScenarioFingerprint}. */
    public ScenarioFingerprint() {}

    /**
     * Computes the fingerprint for a scenario's title.
     *
     * @param scenarioTitle the scenario's display title, e.g. {@code "Scenario: 1.2 - User logs in"}
     * @return the first 16 hex characters of the SHA-256 hash of the normalized scenario name
     */
    public String fingerprint(String scenarioTitle) {
        String normalized = normalize(scenarioTitle);
        return sha256Hex(normalized).substring(0, FINGERPRINT_LENGTH);
    }

    private String normalize(String scenarioTitle) {
        String withoutKeyword = strip(KEYWORD_PREFIX, scenarioTitle);
        String withoutIndex = strip(INDEX_PREFIX, withoutKeyword);
        return withoutIndex.trim().toLowerCase(Locale.ROOT);
    }

    private String strip(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.matches() ? matcher.group(1) : value;
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format(Locale.ROOT, "%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 message digest not available", e);
        }
    }
}
