package com.arc_e_tect.gradle.gherkin.progress;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ScenarioFingerprint")
class ScenarioFingerprintTest {

    private final ScenarioFingerprint fingerprinter = new ScenarioFingerprint();

    @Test
    @DisplayName("produces a 16 hex character fingerprint")
    void producesA16HexCharacterFingerprint() {
        assertThat(fingerprinter.fingerprint("Scenario: User logs in")).matches("[0-9a-f]{16}");
    }

    @Test
    @DisplayName("is stable across repeated calls with the same title")
    void isStableAcrossRepeatedCalls() {
        String first = fingerprinter.fingerprint("Scenario: User logs in");
        String second = fingerprinter.fingerprint("Scenario: User logs in");

        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("ignores the Scenario: keyword prefix")
    void ignoresScenarioKeywordPrefix() {
        String withKeyword = fingerprinter.fingerprint("Scenario: User logs in");
        String withoutKeyword = fingerprinter.fingerprint("User logs in");

        assertThat(withKeyword).isEqualTo(withoutKeyword);
    }

    @Test
    @DisplayName("ignores the Scenario Outline: keyword prefix")
    void ignoresScenarioOutlineKeywordPrefix() {
        String withKeyword = fingerprinter.fingerprint("Scenario Outline: User logs in");
        String withoutKeyword = fingerprinter.fingerprint("User logs in");

        assertThat(withKeyword).isEqualTo(withoutKeyword);
    }

    @Test
    @DisplayName("ignores an existing single-number index prefix")
    void ignoresExistingSingleNumberIndexPrefix() {
        String withIndex = fingerprinter.fingerprint("Scenario: 3 - User logs in");
        String withoutIndex = fingerprinter.fingerprint("Scenario: User logs in");

        assertThat(withIndex).isEqualTo(withoutIndex);
    }

    @Test
    @DisplayName("ignores an existing feature.scenario index prefix")
    void ignoresExistingFeatureScenarioIndexPrefix() {
        String withIndex = fingerprinter.fingerprint("Scenario: 1.2 - User logs in");
        String withoutIndex = fingerprinter.fingerprint("Scenario: User logs in");

        assertThat(withIndex).isEqualTo(withoutIndex);
    }

    @Test
    @DisplayName("is case-insensitive")
    void isCaseInsensitive() {
        String lower = fingerprinter.fingerprint("Scenario: user logs in");
        String upper = fingerprinter.fingerprint("Scenario: USER LOGS IN");

        assertThat(lower).isEqualTo(upper);
    }

    @Test
    @DisplayName("is insensitive to leading and trailing whitespace around the name")
    void isInsensitiveToSurroundingWhitespace() {
        String padded = fingerprinter.fingerprint("Scenario:   User logs in  ");
        String trimmed = fingerprinter.fingerprint("Scenario: User logs in");

        assertThat(padded).isEqualTo(trimmed);
    }

    @Test
    @DisplayName("produces different fingerprints for different scenario names")
    void producesDifferentFingerprintsForDifferentNames() {
        String first = fingerprinter.fingerprint("Scenario: User logs in");
        String second = fingerprinter.fingerprint("Scenario: User logs out");

        assertThat(first).isNotEqualTo(second);
    }
}
