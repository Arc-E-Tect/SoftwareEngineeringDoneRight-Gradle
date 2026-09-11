package com.arc_e_tect.gradle.gherkin.progress;

import com.arc_e_tect.gradle.gherkin.parser.ScenarioInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReinterpretedOutlines")
class ReinterpretedOutlinesTest {

    private static final String OUTLINE_TITLE = "Scenario Outline: User logs in as <username>";

    private ReinterpretedOutlines reinterpretedOutlines;
    private ScenarioFingerprint fingerprinter;

    @BeforeEach
    void setUp() {
        reinterpretedOutlines = new ReinterpretedOutlines();
        fingerprinter = new ScenarioFingerprint();
    }

    @Test
    @DisplayName("finds an outline whose persisted record no longer matches any scenario's title")
    void findsOutlineWhosePersistedRecordNoLongerMatchesAnyScenario() {
        List<ScenarioInfo> scenarios = List.of(row("alice"), row("bob"));

        List<ReinterpretedOutlines.Outline> found =
                reinterpretedOutlines.find(historyFor(OUTLINE_TITLE, null), scenarios);

        assertThat(found).containsExactly(
                new ReinterpretedOutlines.Outline(OUTLINE_TITLE, "User authentication", 2));
    }

    @Test
    @DisplayName("ignores an outline that has no previously persisted record")
    void ignoresOutlineWithoutPreviouslyPersistedRecord() {
        List<ReinterpretedOutlines.Outline> found =
                reinterpretedOutlines.find(Map.of(), List.of(row("alice"), row("bob")));

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("ignores an outline whose persisted record is already marked removed")
    void ignoresOutlineWhoseRecordIsAlreadyMarkedRemoved() {
        Map<String, ScenarioProgressRecord> previous = historyFor(OUTLINE_TITLE, Instant.parse("2026-09-01T00:00:00Z"));

        List<ReinterpretedOutlines.Outline> found =
                reinterpretedOutlines.find(previous, List.of(row("alice"), row("bob")));

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("ignores an outline still reported under its own unsubstituted title")
    void ignoresOutlineStillReportedUnderItsOwnTitle() {
        // A single Examples row of an outline whose name holds no placeholder keeps the outline's
        // own title, so its record is still being advanced and nothing has been superseded.
        ScenarioInfo onlyRow = new ScenarioInfo(
                "User authentication", "Scenario Outline: User logs in", List.of("the login page is open"),
                "Scenario Outline: User logs in");

        List<ReinterpretedOutlines.Outline> found =
                reinterpretedOutlines.find(historyFor("Scenario Outline: User logs in", null), List.of(onlyRow));

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("ignores scenarios that are not expanded Examples rows")
    void ignoresScenariosThatAreNotExpandedExamplesRows() {
        ScenarioInfo plain = new ScenarioInfo(
                "User authentication", "Scenario: User logs in", List.of("the login page is open"));

        List<ReinterpretedOutlines.Outline> found =
                reinterpretedOutlines.find(historyFor("Scenario: User logs in", null), List.of(plain));

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("reports every re-interpreted outline, in the order each was first encountered")
    void reportsEveryReinterpretedOutlineInEncounterOrder() {
        String second = "Scenario Outline: User pays with <method>";
        Map<String, ScenarioProgressRecord> previous = Map.of(
                fingerprinter.fingerprint(OUTLINE_TITLE), record(OUTLINE_TITLE, null),
                fingerprinter.fingerprint(second), record(second, null));
        List<ScenarioInfo> scenarios = List.of(
                row("alice"),
                new ScenarioInfo("Invoice payment", "Scenario Outline: User pays with card", List.of(), second),
                row("bob"));

        List<ReinterpretedOutlines.Outline> found = reinterpretedOutlines.find(previous, scenarios);

        assertThat(found).containsExactly(
                new ReinterpretedOutlines.Outline(OUTLINE_TITLE, "User authentication", 2),
                new ReinterpretedOutlines.Outline(second, "Invoice payment", 1));
    }

    private ScenarioInfo row(String username) {
        return new ScenarioInfo(
                "User authentication", "Scenario Outline: User logs in as " + username,
                List.of("the login page is open"), OUTLINE_TITLE);
    }

    private Map<String, ScenarioProgressRecord> historyFor(String title, Instant removedAt) {
        return Map.of(fingerprinter.fingerprint(title), record(title, removedAt));
    }

    private ScenarioProgressRecord record(String title, Instant removedAt) {
        return new ScenarioProgressRecord(
                fingerprinter.fingerprint(title), title, "User authentication",
                Instant.parse("2026-08-01T00:00:00Z"), null, null,
                Instant.parse("2026-08-01T00:00:00Z"), removedAt);
    }
}
