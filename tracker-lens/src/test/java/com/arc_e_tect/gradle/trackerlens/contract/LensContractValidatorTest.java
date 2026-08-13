package com.arc_e_tect.gradle.trackerlens.contract;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LensContractValidator")
class LensContractValidatorTest {

    @TempDir
    Path tempDir;

    private final LensContractValidator validator = new LensContractValidator();

    @Test
    @DisplayName("validateShouldReturnNoViolationsForConformantDashboard")
    void validateShouldReturnNoViolationsForConformantDashboard() throws IOException {
        Path html = writeFile("conformant.html", """
                <!doctype html>
                <html><body>
                <div class="dashboard">
                  <div class="lens-switcher" data-lens-count="2"><select></select></div>
                  <section class="tracker" data-tracker="bdd-scenarios">
                    <div class="metric-card" data-stage="listed" style="--percent: 100"></div>
                    <div class="metric-card" data-stage="defined" style="--percent: 80"></div>
                    <div class="projection" data-status="low"></div>
                    <div class="chart"><canvas></canvas></div>
                  </section>
                  <section class="tracker" data-tracker="api-contracts">
                    <div class="metric-card" data-stage="declared" style="--percent: 100"></div>
                    <div class="chart"><canvas></canvas></div>
                  </section>
                  <table class="stale-items__table"></table>
                </div>
                </body></html>
                """);

        List<Violation> violations = validator.validate(html.toFile());

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("validateShouldReportExactViolationsForBrokenDashboard")
    void validateShouldReportExactViolationsForBrokenDashboard() throws IOException {
        Path html = writeFile("broken.html", """
                <!doctype html>
                <html><body>
                <div class="dashboard"></div>
                <div class="dashboard">
                  <section class="tracker" data-tracker="bdd-scenarios">
                  </section>
                  <section class="tracker" data-tracker="api-contracts">
                    <div class="metric-card" data-stage="declared"></div>
                    <div class="projection" data-status="low"></div>
                    <div class="projection" data-status="medium"></div>
                    <div class="chart"><canvas></canvas></div>
                  </section>
                  <table class="stale-items__table"></table>
                  <table class="stale-items__table"></table>
                  <div class="lens-switcher" data-lens-count="1"></div>
                  <div class="lens-switcher" data-lens-count="1"></div>
                </div>
                </body></html>
                """);

        List<Violation> violations = validator.validate(html.toFile());

        assertThat(violations).extracting(Violation::rule).containsExactly(
                ContractRule.DASHBOARD_ROOT,
                ContractRule.METRIC_CARD,
                ContractRule.METRIC_CARD_PERCENT_HOOK,
                ContractRule.PROJECTION_BLOCK,
                ContractRule.CHART_CANVAS,
                ContractRule.STALE_TABLE,
                ContractRule.LENS_SWITCHER);
    }

    @Test
    @DisplayName("validateShouldIdentifyWhichTrackerFailedAPerTrackerRule")
    void validateShouldIdentifyWhichTrackerFailedAPerTrackerRule() throws IOException {
        Path html = writeFile("missing-metric.html", """
                <!doctype html>
                <html><body>
                <div class="dashboard">
                  <section class="tracker" data-tracker="bdd-scenarios"></section>
                </div>
                </body></html>
                """);

        List<Violation> violations = validator.validate(html.toFile());

        assertThat(violations)
                .filteredOn(v -> v.rule() == ContractRule.METRIC_CARD)
                .extracting(Violation::message)
                .containsExactly("tracker 'bdd-scenarios': expected at least one match, found 0");
    }

    private Path writeFile(String name, String content) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content);
        return file;
    }
}
