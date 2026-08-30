package com.arc_e_tect.gradle.gherkin.indexing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FeatureIndexer")
class FeatureIndexerTest {

    @TempDir
    Path tempDir;

    private final FeatureIndexer indexer = new FeatureIndexer();

    @Test
    @DisplayName("mode OFF leaves feature and scenario titles untouched")
    void offModeLeavesTitlesUntouched() throws IOException {
        File file = writeFeature("authentication.feature",
                "Feature: User authentication\n\n  Scenario: User logs in\n    Given a user\n");

        indexer.reindex(List.of(file), IndexingMode.OFF, false);

        assertThat(content(file))
                .contains("Feature: User authentication")
                .contains("Scenario: User logs in");
    }

    @Test
    @DisplayName("mode FEATURE numbers features in the order given, leaves scenarios untouched")
    void featureModeNumbersFeaturesInGivenOrder() throws IOException {
        File auth = writeFeature("authentication.feature",
                "Feature: User authentication\n\n  Scenario: User logs in\n    Given a user\n");
        File invoice = writeFeature("invoice.feature",
                "Feature: Invoice payment\n\n  Scenario: User pays an invoice\n    Given an invoice\n");

        indexer.reindex(List.of(auth, invoice), IndexingMode.FEATURE, false);

        assertThat(content(auth))
                .contains("Feature: 1 - User authentication")
                .contains("Scenario: User logs in");
        assertThat(content(invoice))
                .contains("Feature: 2 - Invoice payment")
                .contains("Scenario: User pays an invoice");
    }

    @Test
    @DisplayName("mode SCENARIO numbers scenarios continuously across files, leaves features untouched")
    void scenarioModeNumbersScenariosContinuously() throws IOException {
        File auth = writeFeature("authentication.feature", """
                Feature: User authentication

                  Scenario: User logs in
                    Given a user

                  Scenario: User resets password
                    Given a user
                """);
        File invoice = writeFeature("invoice.feature",
                "Feature: Invoice payment\n\n  Scenario: User pays an invoice\n    Given an invoice\n");

        indexer.reindex(List.of(auth, invoice), IndexingMode.SCENARIO, false);

        assertThat(content(auth))
                .contains("Feature: User authentication")
                .contains("Scenario: 1 - User logs in")
                .contains("Scenario: 2 - User resets password");
        assertThat(content(invoice))
                .contains("Feature: Invoice payment")
                .contains("Scenario: 3 - User pays an invoice");
    }

    @Test
    @DisplayName("mode ALL numbers features and numbers scenarios per feature as featureNumber.scenarioNumber")
    void allModeNumbersFeaturesAndScenariosPerFeature() throws IOException {
        File auth = writeFeature("authentication.feature", """
                Feature: User authentication

                  Scenario: User logs in
                    Given a user

                  Scenario: User resets password
                    Given a user
                """);
        File invoice = writeFeature("invoice.feature",
                "Feature: Invoice payment\n\n  Scenario: User pays an invoice\n    Given an invoice\n");

        indexer.reindex(List.of(auth, invoice), IndexingMode.ALL, false);

        assertThat(content(auth))
                .contains("Feature: 1 - User authentication")
                .contains("Scenario: 1.1 - User logs in")
                .contains("Scenario: 1.2 - User resets password");
        assertThat(content(invoice))
                .contains("Feature: 2 - Invoice payment")
                .contains("Scenario: 2.1 - User pays an invoice");
    }

    @Test
    @DisplayName("numbers files in the order given, not re-sorted alphabetically")
    void numbersFilesInGivenOrderNotAlphabetically() throws IOException {
        File zFile = writeFeature("z.feature",
                "Feature: Z Feature\n\n  Scenario: Z scenario\n    Given z\n");
        File aFile = writeFeature("a.feature",
                "Feature: A Feature\n\n  Scenario: A scenario\n    Given a\n");

        // Given in z-then-a order: the caller (not the indexer) is responsible for ordering.
        indexer.reindex(List.of(zFile, aFile), IndexingMode.FEATURE, false);

        assertThat(content(zFile)).contains("Feature: 1 - Z Feature");
        assertThat(content(aFile)).contains("Feature: 2 - A Feature");
    }

    @Test
    @DisplayName("numbers Scenario Outline the same as Scenario")
    void numbersScenarioOutline() throws IOException {
        File file = writeFeature("outline.feature", """
                Feature: Sample

                  Scenario Outline: User logs in with <role>
                    Given a "<role>" user

                    Examples:
                      | role  |
                      | admin |
                """);

        indexer.reindex(List.of(file), IndexingMode.ALL, false);

        assertThat(content(file)).contains("Scenario Outline: 1.1 - User logs in with <role>");
    }

    @Test
    @DisplayName("numbers scenarios nested inside a Rule block, preserving their indentation")
    void numbersScenariosInsideRule() throws IOException {
        File file = writeFeature("rules.feature", """
                Feature: Rule-Based Scenarios

                  Rule: Registered users can access premium content

                    Scenario: Premium user views protected page
                      Given a premium user
                """);

        indexer.reindex(List.of(file), IndexingMode.SCENARIO, false);

        assertThat(content(file)).contains("    Scenario: 1 - Premium user views protected page");
    }

    @Test
    @DisplayName("switching from ALL to OFF removes all numbering")
    void switchingToOffRemovesNumbering() throws IOException {
        File auth = writeFeature("authentication.feature",
                "Feature: User authentication\n\n  Scenario: User logs in\n    Given a user\n");
        File invoice = writeFeature("invoice.feature",
                "Feature: Invoice payment\n\n  Scenario: User pays an invoice\n    Given an invoice\n");
        indexer.reindex(List.of(auth, invoice), IndexingMode.ALL, false);

        indexer.reindex(List.of(auth, invoice), IndexingMode.OFF, false);

        assertThat(content(auth))
                .contains("Feature: User authentication")
                .contains("Scenario: User logs in")
                .doesNotContain("1 -")
                .doesNotContain("1.1 -");
        assertThat(content(invoice))
                .contains("Feature: Invoice payment")
                .contains("Scenario: User pays an invoice");
    }

    @Test
    @DisplayName("switching from SCENARIO to FEATURE removes scenario numbers and adds feature numbers")
    void switchingModesReplacesNumbering() throws IOException {
        File auth = writeFeature("authentication.feature",
                "Feature: User authentication\n\n  Scenario: User logs in\n    Given a user\n");
        indexer.reindex(List.of(auth), IndexingMode.SCENARIO, false);
        assertThat(content(auth)).contains("Scenario: 1 - User logs in");

        indexer.reindex(List.of(auth), IndexingMode.FEATURE, false);

        assertThat(content(auth))
                .contains("Feature: 1 - User authentication")
                .contains("Scenario: User logs in")
                .doesNotContain("Scenario: 1 -");
    }

    @Test
    @DisplayName("re-running the same mode is idempotent and does not change file content")
    void reindexingWithSameModeIsIdempotent() throws IOException {
        File auth = writeFeature("authentication.feature",
                "Feature: User authentication\n\n  Scenario: User logs in\n    Given a user\n");
        indexer.reindex(List.of(auth), IndexingMode.ALL, false);
        String firstPass = content(auth);

        indexer.reindex(List.of(auth), IndexingMode.ALL, false);

        assertThat(content(auth)).isEqualTo(firstPass);
    }

    @Test
    @DisplayName("does not renumber unrelated lines that merely contain the word Scenario")
    void doesNotTouchUnrelatedLines() throws IOException {
        File file = writeFeature("sample.feature", """
                Feature: Sample

                  Scenario: User logs in
                    Given a user with role "Scenario: not a keyword"
                """);

        indexer.reindex(List.of(file), IndexingMode.SCENARIO, false);

        assertThat(content(file))
                .contains("Scenario: 1 - User logs in")
                .contains("Given a user with role \"Scenario: not a keyword\"");
    }

    // --- forceRewrite = false (default): preserve already-correctly-numbered lines ---

    @Test
    @DisplayName("forceRewrite false: a new alphabetically-earlier file does not steal an already-numbered "
            + "file's number")
    void unpinnedFileDoesNotStealAlreadyNumberedFilesNumber() throws IOException {
        File zFile = writeFeature("z.feature",
                "Feature: 1 - Z Feature\n\n  Scenario: Z scenario\n    Given z\n");
        File aFile = writeFeature("a.feature",
                "Feature: A Feature\n\n  Scenario: A scenario\n    Given a\n");

        // Given in correct alphabetical order: a before z.
        indexer.reindex(List.of(aFile, zFile), IndexingMode.FEATURE, false);

        assertThat(content(zFile)).contains("Feature: 1 - Z Feature");
        assertThat(content(aFile)).contains("Feature: 2 - A Feature");
    }

    @Test
    @DisplayName("forceRewrite true: an already-numbered file is renumbered to fit alphabetical order, "
            + "same as before this property existed")
    void forceRewriteTrueRenumbersEverythingFromScratch() throws IOException {
        File zFile = writeFeature("z.feature",
                "Feature: 1 - Z Feature\n\n  Scenario: Z scenario\n    Given z\n");
        File aFile = writeFeature("a.feature",
                "Feature: A Feature\n\n  Scenario: A scenario\n    Given a\n");

        indexer.reindex(List.of(aFile, zFile), IndexingMode.FEATURE, true);

        assertThat(content(aFile)).contains("Feature: 1 - A Feature");
        assertThat(content(zFile)).contains("Feature: 2 - Z Feature");
    }

    @Test
    @DisplayName("forceRewrite false: a scenario number in the wrong format for the current mode is "
            + "renumbered, e.g. switching from SCENARIO to ALL")
    void scenarioNumberNotMatchingNewModeFormatIsRenumbered() throws IOException {
        File file = writeFeature("authentication.feature",
                "Feature: User authentication\n\n  Scenario: User logs in\n    Given a user\n");
        indexer.reindex(List.of(file), IndexingMode.SCENARIO, false);
        assertThat(content(file)).contains("Scenario: 1 - User logs in");

        indexer.reindex(List.of(file), IndexingMode.ALL, false);

        assertThat(content(file))
                .contains("Feature: 1 - User authentication")
                .contains("Scenario: 1.1 - User logs in")
                .doesNotContain("Scenario: 1 -");
    }

    @Test
    @DisplayName("forceRewrite false: a feature number left over from a mode that doesn't number features "
            + "is stripped, e.g. switching from ALL to SCENARIO")
    void featureNumberNotExpectedByNewModeIsStripped() throws IOException {
        File file = writeFeature("authentication.feature",
                "Feature: User authentication\n\n  Scenario: User logs in\n    Given a user\n");
        indexer.reindex(List.of(file), IndexingMode.ALL, false);
        assertThat(content(file)).contains("Feature: 1 - User authentication");

        indexer.reindex(List.of(file), IndexingMode.SCENARIO, false);

        assertThat(content(file))
                .contains("Feature: User authentication")
                .contains("Scenario: 1 - User logs in");
    }

    @Test
    @DisplayName("forceRewrite false: an already-numbered scenario keeps its number; only the unnumbered "
            + "one gets a fresh number, not colliding with the pinned one")
    void pinnedScenarioKeepsItsNumberUnnumberedOneGetsNextAvailable() throws IOException {
        File file = writeFeature("sample.feature", """
                Feature: Sample

                  Scenario: 5 - Already numbered
                    Given a user

                  Scenario: Not yet numbered
                    Given a user
                """);

        indexer.reindex(List.of(file), IndexingMode.SCENARIO, false);

        assertThat(content(file))
                .contains("Scenario: 5 - Already numbered")
                .contains("Scenario: 6 - Not yet numbered");
    }

    @Test
    @DisplayName("forceRewrite false: re-running the same mode twice is idempotent, same as before this "
            + "property existed")
    void forceRewriteFalseIsIdempotentAcrossRuns() throws IOException {
        File auth = writeFeature("authentication.feature",
                "Feature: User authentication\n\n  Scenario: User logs in\n    Given a user\n");
        indexer.reindex(List.of(auth), IndexingMode.ALL, false);
        String firstPass = content(auth);

        indexer.reindex(List.of(auth), IndexingMode.ALL, false);

        assertThat(content(auth)).isEqualTo(firstPass);
    }

    @Test
    @DisplayName("forceRewrite false: an ALL-mode scenario number matching a different feature number than "
            + "its own resolved one is renumbered")
    void scenarioNumberMismatchedWithOwnFeatureNumberIsRenumbered() throws IOException {
        // "3.1" would only be pinned if this feature's own resolved number were 3 - it isn't (no
        // other feature is numbered here, so this one resolves to 1), so it must be renumbered.
        File file = writeFeature("sample.feature",
                "Feature: Sample\n\n  Scenario: 3.1 - Stale scenario\n    Given g\n");

        indexer.reindex(List.of(file), IndexingMode.ALL, false);

        assertThat(content(file))
                .contains("Feature: 1 - Sample")
                .contains("Scenario: 1.1 - Stale scenario");
    }

    @Test
    @DisplayName("forceRewrite false: a scenario moved from one already-numbered feature to another keeps "
            + "its old feature's number pinned only in the old feature - in the new feature it's renumbered "
            + "to follow that feature's own already-pinned scenarios")
    void scenarioMovedToAnotherFeatureIsRenumberedForItsNewFeature() throws IOException {
        // "1.1" was this scenario's number back when it lived in feature 1; feature 2 already has
        // its own "2.1", so the moved-in scenario must not keep the stale "1.1" - it should become
        // "2.2", continuing feature 2's own sequence.
        File auth = writeFeature("authentication.feature",
                "Feature: 1 - User authentication\n\n  Scenario: Some other scenario\n    Given a user\n");
        File invoice = writeFeature("invoice.feature", """
                Feature: 2 - Invoice payment

                  Scenario: 2.1 - User pays an invoice
                    Given an invoice

                  Scenario: 1.1 - User logs in
                    Given a user
                """);

        indexer.reindex(List.of(auth, invoice), IndexingMode.ALL, false);

        assertThat(content(invoice))
                .contains("Feature: 2 - Invoice payment")
                .contains("Scenario: 2.1 - User pays an invoice")
                .contains("Scenario: 2.2 - User logs in");
    }

    @Test
    @DisplayName("forceRewrite false: two features independently numbered with the same number - e.g. after "
            + "merging branches maintained concurrently - only the first keeps that number, the second is "
            + "reindexed to the next available one")
    void collidingFeatureNumbersAreReindexed() throws IOException {
        File auth = writeFeature("authentication.feature",
                "Feature: 1 - User authentication\n\n  Scenario: User logs in\n    Given a user\n");
        File invoice = writeFeature("invoice.feature",
                "Feature: 1 - Invoice payment\n\n  Scenario: User pays an invoice\n    Given an invoice\n");

        indexer.reindex(List.of(auth, invoice), IndexingMode.FEATURE, false);

        assertThat(content(auth)).contains("Feature: 1 - User authentication");
        assertThat(content(invoice)).contains("Feature: 2 - Invoice payment");
    }

    @Test
    @DisplayName("forceRewrite false: three features independently numbered with the same number are "
            + "reindexed so all three end up unique, not just the second")
    void multipleCollidingFeatureNumbersAllEndUpUnique() throws IOException {
        File first = writeFeature("a.feature",
                "Feature: 1 - Feature A\n\n  Scenario: A scenario\n    Given a\n");
        File second = writeFeature("b.feature",
                "Feature: 1 - Feature B\n\n  Scenario: B scenario\n    Given b\n");
        File third = writeFeature("c.feature",
                "Feature: 1 - Feature C\n\n  Scenario: C scenario\n    Given c\n");

        indexer.reindex(List.of(first, second, third), IndexingMode.FEATURE, false);

        assertThat(content(first)).contains("Feature: 1 - Feature A");
        assertThat(content(second)).contains("Feature: 2 - Feature B");
        assertThat(content(third)).contains("Feature: 3 - Feature C");
    }

    @Test
    @DisplayName("forceRewrite false: two scenarios within the same feature independently numbered with the "
            + "same number - e.g. after merging branches maintained concurrently - only the first keeps that "
            + "number, the second is reindexed")
    void collidingScenarioNumbersWithinSameFeatureAreReindexed() throws IOException {
        File file = writeFeature("sample.feature", """
                Feature: Sample

                  Scenario: 1 - First branch's scenario
                    Given a user

                  Scenario: 1 - Second branch's scenario
                    Given a user
                """);

        indexer.reindex(List.of(file), IndexingMode.SCENARIO, false);

        assertThat(content(file))
                .contains("Scenario: 1 - First branch's scenario")
                .contains("Scenario: 2 - Second branch's scenario");
    }

    @Test
    @DisplayName("forceRewrite false: colliding ALL-mode scenario numbers within the same feature are "
            + "reindexed, keeping the feature.scenario format")
    void collidingAllModeScenarioNumbersWithinSameFeatureAreReindexed() throws IOException {
        File file = writeFeature("sample.feature", """
                Feature: 1 - Sample

                  Scenario: 1.2 - First branch's scenario
                    Given a user

                  Scenario: 1.2 - Second branch's scenario
                    Given a user
                """);

        indexer.reindex(List.of(file), IndexingMode.ALL, false);

        assertThat(content(file))
                .contains("Feature: 1 - Sample")
                .contains("Scenario: 1.2 - First branch's scenario")
                .contains("Scenario: 1.3 - Second branch's scenario");
    }

    // --- reindex(..., List<File> projectDirectories, Runnable) overload: multi-project scoping ---

    @Test
    @DisplayName("empty projectDirectories numbers every file as one continuous sequence, same as the "
            + "overloads that don't accept projectDirectories at all")
    void emptyProjectDirectoriesTreatsEveryFileAsOneGroup() throws IOException {
        File projA = projectDir("projA");
        File projB = projectDir("projB");
        File a = writeFeatureIn(projA, "a.feature",
                "Feature: A Feature\n\n  Scenario: A scenario\n    Given a\n");
        File b = writeFeatureIn(projB, "b.feature",
                "Feature: B Feature\n\n  Scenario: B scenario\n    Given b\n");

        indexer.reindex(List.of(a, b), IndexingMode.FEATURE, false, List.of(), () -> { });

        assertThat(content(a)).contains("Feature: 1 - A Feature");
        assertThat(content(b)).contains("Feature: 2 - B Feature");
    }

    @Test
    @DisplayName("non-empty projectDirectories scopes FEATURE numbering to each project, so every "
            + "project's own features start at 1 independently of every other project's")
    void projectDirectoriesResetsFeatureNumberingPerProject() throws IOException {
        File projA = projectDir("projA");
        File projB = projectDir("projB");
        File a1 = writeFeatureIn(projA, "a1.feature",
                "Feature: First A Feature\n\n  Scenario: A scenario\n    Given a\n");
        File a2 = writeFeatureIn(projA, "a2.feature",
                "Feature: Second A Feature\n\n  Scenario: A scenario\n    Given a\n");
        File b = writeFeatureIn(projB, "b.feature",
                "Feature: B Feature\n\n  Scenario: B scenario\n    Given b\n");

        indexer.reindex(List.of(a1, a2, b), IndexingMode.FEATURE, false, List.of(projA, projB), () -> { });

        assertThat(content(a1)).contains("Feature: 1 - First A Feature");
        assertThat(content(a2)).contains("Feature: 2 - Second A Feature");
        assertThat(content(b)).contains("Feature: 1 - B Feature");
    }

    @Test
    @DisplayName("non-empty projectDirectories scopes SCENARIO mode's cross-file numbering to each "
            + "project the same way it scopes FEATURE numbering")
    void projectDirectoriesResetsScenarioNumberingPerProjectInScenarioMode() throws IOException {
        File projA = projectDir("projA");
        File projB = projectDir("projB");
        File a = writeFeatureIn(projA, "a.feature", """
                Feature: A Feature

                  Scenario: First A scenario
                    Given a

                  Scenario: Second A scenario
                    Given a
                """);
        File b = writeFeatureIn(projB, "b.feature",
                "Feature: B Feature\n\n  Scenario: B scenario\n    Given b\n");

        indexer.reindex(List.of(a, b), IndexingMode.SCENARIO, false, List.of(projA, projB), () -> { });

        assertThat(content(a))
                .contains("Scenario: 1 - First A scenario")
                .contains("Scenario: 2 - Second A scenario");
        assertThat(content(b)).contains("Scenario: 1 - B scenario");
    }

    @Test
    @DisplayName("non-empty projectDirectories doesn't affect ALL mode's Scenario numbering: it's already "
            + "scoped per Feature, strictly finer-grained than per-project")
    void allModeScenarioNumberingUnaffectedByProjectDirectories() throws IOException {
        File projA = projectDir("projA");
        File a = writeFeatureIn(projA, "a.feature", """
                Feature: A Feature

                  Scenario: First A scenario
                    Given a

                  Scenario: Second A scenario
                    Given a
                """);

        indexer.reindex(List.of(a), IndexingMode.ALL, false, List.of(projA), () -> { });

        assertThat(content(a))
                .contains("Feature: 1 - A Feature")
                .contains("Scenario: 1.1 - First A scenario")
                .contains("Scenario: 1.2 - Second A scenario");
    }

    @Test
    @DisplayName("a feature file that isn't under any of projectDirectories becomes its own single-file "
            + "group instead of being folded into an unrelated project's numbering")
    void fileNotUnderAnyProjectDirectoryBecomesItsOwnGroup() throws IOException {
        File projA = projectDir("projA");
        File orphan = writeFeature("orphan.feature",
                "Feature: Orphan Feature\n\n  Scenario: Orphan scenario\n    Given o\n");
        File a = writeFeatureIn(projA, "a.feature",
                "Feature: A Feature\n\n  Scenario: A scenario\n    Given a\n");

        indexer.reindex(List.of(orphan, a), IndexingMode.FEATURE, false, List.of(projA), () -> { });

        assertThat(content(orphan)).contains("Feature: 1 - Orphan Feature");
        assertThat(content(a)).contains("Feature: 1 - A Feature");
    }

    // --- reindex(..., Runnable) callback overload ---

    @Test
    @DisplayName("callback overload invokes the callback exactly once per feature file")
    void callbackOverloadInvokesCallbackOncePerFile() throws IOException {
        File auth = writeFeature("authentication.feature",
                "Feature: User authentication\n\n  Scenario: User logs in\n    Given a user\n");
        File invoice = writeFeature("invoice.feature",
                "Feature: Invoice payment\n\n  Scenario: User pays an invoice\n    Given an invoice\n");
        AtomicInteger callbackCount = new AtomicInteger();

        indexer.reindex(List.of(auth, invoice), IndexingMode.FEATURE, false, callbackCount::incrementAndGet);

        assertThat(callbackCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("callback overload leaves file content exactly as the no-callback overload would")
    void callbackOverloadBehavesExactlyLikeNoCallbackOverload() throws IOException {
        File file = writeFeature("authentication.feature",
                "Feature: User authentication\n\n  Scenario: User logs in\n    Given a user\n");

        indexer.reindex(List.of(file), IndexingMode.ALL, false, () -> { });

        assertThat(content(file)).contains("Feature: 1 - User authentication", "Scenario: 1.1 - User logs in");
    }

    @Test
    @DisplayName("no-callback overload still behaves exactly as before this overload existed")
    void noCallbackOverloadUnaffectedByCallbackOverloadExisting() throws IOException {
        File file = writeFeature("authentication.feature",
                "Feature: User authentication\n\n  Scenario: User logs in\n    Given a user\n");

        indexer.reindex(List.of(file), IndexingMode.ALL, false);

        assertThat(content(file)).contains("Feature: 1 - User authentication", "Scenario: 1.1 - User logs in");
    }

    private File writeFeature(String name, String content) throws IOException {
        File file = tempDir.resolve(name).toFile();
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        return file;
    }

    private File projectDir(String name) throws IOException {
        Path dir = tempDir.resolve(name);
        Files.createDirectories(dir);
        return dir.toFile();
    }

    private File writeFeatureIn(File dir, String name, String content) throws IOException {
        File file = new File(dir, name);
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        return file;
    }

    private String content(File file) throws IOException {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }
}
