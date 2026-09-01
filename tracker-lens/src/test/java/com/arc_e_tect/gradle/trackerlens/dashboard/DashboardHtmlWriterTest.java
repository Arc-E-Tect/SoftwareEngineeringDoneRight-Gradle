package com.arc_e_tect.gradle.trackerlens.dashboard;

import com.arc_e_tect.gradle.trackerlens.contract.LensContractValidator;
import com.arc_e_tect.gradle.trackerlens.contract.Violation;
import com.arc_e_tect.gradle.trackerlens.lens.ResolvedLens;
import com.arc_e_tect.gradle.trackerlens.projection.Confidence;
import com.arc_e_tect.gradle.trackerlens.projection.Projection;
import com.arc_e_tect.gradle.trackerlens.tracker.LifecycleRecord;
import org.gradle.api.GradleException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DashboardHtmlWriter")
class DashboardHtmlWriterTest {

    private static final Instant NOW = Instant.parse("2026-08-13T00:00:00Z");

    @TempDir
    Path tempDir;

    private final DashboardHtmlWriter writer = new DashboardHtmlWriter();
    private final LensContractValidator validator = new LensContractValidator();

    @Test
    @DisplayName("writeShouldProduceDashboardThatPassesItsOwnContractValidation")
    void writeShouldProduceDashboardThatPassesItsOwnContractValidation() {
        DashboardView view = twoTrackerView();

        File dashboardFile = writer.write(tempDir.toFile(), view, null);

        List<Violation> violations = validator.validate(dashboardFile);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("writeShouldRenderDashboardNameAndVersionInTitleAndHeading")
    void writeShouldRenderDashboardNameAndVersionInTitleAndHeading() throws IOException {
        DashboardView view = twoTrackerView();

        File dashboardFile = writer.write(tempDir.toFile(), view, null);

        Document document = Jsoup.parse(dashboardFile, "UTF-8");
        assertThat(document.title()).isEqualTo("my-app Lens");
        assertThat(document.select("h1").text()).isEqualTo("my-app Lens");
        assertThat(document.select(".dashboard-version").text()).contains("1.2.3");
    }

    @Test
    @DisplayName("writeShouldWriteOneCssFilePerLensWithNamespacedFileName")
    void writeShouldWriteOneCssFilePerLensWithNamespacedFileName() {
        DashboardView view = twoTrackerView();

        writer.write(tempDir.toFile(), view, null);

        assertThat(new File(tempDir.toFile(), "dark-lens-midnight-theme.css")).exists();
    }

    @Test
    @DisplayName("writeShouldOmitProjectionElementForTrackerWithoutProjection")
    void writeShouldOmitProjectionElementForTrackerWithoutProjection() throws IOException {
        DashboardView view = twoTrackerView();

        File dashboardFile = writer.write(tempDir.toFile(), view, null);

        Document document = Jsoup.parse(dashboardFile, "UTF-8");
        assertThat(document.select(".tracker[data-tracker=\"api-contracts\"] .projection")).isEmpty();
    }

    @Test
    @DisplayName("writeShouldRenderARemovedMetricCard")
    void writeShouldRenderARemovedMetricCard() throws IOException {
        List<LifecycleRecord> records = List.of(
                new LifecycleRecord("g1", "Place an order", null, Map.of("listed", NOW), NOW, null),
                new LifecycleRecord("g2", "Descoped scenario", null, Map.of("listed", NOW), NOW, NOW));
        TrackerView trackerView = new TrackerViewFactory().build(
                "bdd-scenarios", List.of("listed", "defined", "implemented"), records, Optional.empty(), NOW, true);
        DashboardView view = new DashboardView(List.of(trackerView),
                List.of(new ResolvedLens("light-lens", "built-in", "body{}".getBytes(StandardCharsets.UTF_8))),
                "light-lens", "my-app Lens", "1.2.3");

        File dashboardFile = writer.write(tempDir.toFile(), view, null);

        Document document = Jsoup.parse(dashboardFile, "UTF-8");
        var removedCard = document.select(".metric-card[data-stage=\"removed\"]");
        assertThat(removedCard).hasSize(1);
        assertThat(removedCard.select(".metric-card__count").text()).isEqualTo("1 / 2");
    }

    @Test
    @DisplayName("writeShouldRenderStaleItemsTableRowPerStaleItemAcrossTrackers")
    void writeShouldRenderStaleItemsTableRowPerStaleItemAcrossTrackers() throws IOException {
        LifecycleRecord staleWithLastSeen = new LifecycleRecord("s1", "Old scenario", "Legacy",
                Map.of("listed", NOW.minus(Duration.ofDays(40))), NOW.minus(Duration.ofDays(40)), null);
        LifecycleRecord staleWithoutLastSeen = new LifecycleRecord("s2", "Older scenario", null,
                Map.of("listed", NOW.minus(Duration.ofDays(40))), null, null);
        TrackerView trackerWithStale = new TrackerView(
                "bdd-scenarios", List.of("listed"), List.of(new MetricCardView("listed", 2, 2, 100)), 2,
                Optional.empty(), List.of(), Map.of(), List.of(staleWithLastSeen, staleWithoutLastSeen),
                Map.of("listed", 2), List.of());

        File dashboardFile = writer.write(tempDir.toFile(), new DashboardView(
                List.of(trackerWithStale),
                List.of(new ResolvedLens("light-lens", "built-in", "body{}".getBytes(StandardCharsets.UTF_8))),
                "light-lens", "my-app Lens", "1.2.3"), null);

        Document document = Jsoup.parse(dashboardFile, "UTF-8");
        assertThat(document.select(".stale-items__table tbody tr")).hasSize(2);
    }

    @Test
    @DisplayName("writeShouldVaryDisclaimerTextByConfidenceTier")
    void writeShouldVaryDisclaimerTextByConfidenceTier() throws IOException {
        TrackerView medium = trackerWithProjection("t-medium", Confidence.MEDIUM);
        TrackerView high = trackerWithProjection("t-high", Confidence.HIGH);

        File dashboardFile = writer.write(tempDir.toFile(), new DashboardView(
                List.of(medium, high),
                List.of(new ResolvedLens("light-lens", "built-in", "body{}".getBytes(StandardCharsets.UTF_8))),
                "light-lens", "my-app Lens", "1.2.3"), null);

        Document document = Jsoup.parse(dashboardFile, "UTF-8");
        assertThat(document.select(".tracker[data-tracker=\"t-medium\"] .projection__disclaimer").text())
                .contains("growing window");
        assertThat(document.select(".tracker[data-tracker=\"t-high\"] .projection__disclaimer").text())
                .contains("full 90-day window");
    }

    @Test
    @DisplayName("writeShouldRenderStaticTextFromACustomTemplateInsteadOfTheBundledDefault")
    void writeShouldRenderStaticTextFromACustomTemplateInsteadOfTheBundledDefault() throws IOException {
        Path template = tempDir.resolve("dashboard-template.html");
        Files.writeString(template, """
                <!doctype html>
                <html lang="fr"><head><meta charset="utf-8"><title>Tableau de bord</title>
                <link rel="stylesheet" id="lens-stylesheet" href="{{defaultLensCssFile}}"></head>
                <body>
                <div class="dashboard">
                <h1>Tableau de bord Tracker Lens</h1>
                <div class="lens-switcher" data-lens-count="{{lensCount}}"><select></select></div>
                {{#trackers}}
                <section class="tracker" data-tracker="{{id}}">
                {{#metrics}}<div class="metric-card" data-stage="{{stage}}" style="--percent: {{percent}}"></div>{{/metrics}}
                <div class="chart"><canvas></canvas></div>
                </section>
                {{/trackers}}
                </div>
                {{{dashboardDataScript}}}
                </body></html>
                """);
        DashboardView view = twoTrackerView();

        File dashboardFile = writer.write(tempDir.toFile(), view, template.toFile());

        String html = Files.readString(dashboardFile.toPath());
        assertThat(html).contains("Tableau de bord Tracker Lens");
    }

    @Test
    @DisplayName("writeShouldValidateACustomTemplatesOutputAgainstTheContractJustLikeTheDefault")
    void writeShouldValidateACustomTemplatesOutputAgainstTheContractJustLikeTheDefault() throws IOException {
        Path template = tempDir.resolve("dashboard-template.html");
        Files.writeString(template, """
                <!doctype html>
                <html><head><link rel="stylesheet" id="lens-stylesheet" href="{{defaultLensCssFile}}"></head>
                <body>
                <div class="dashboard">
                <h1>Custom</h1>
                <div class="lens-switcher" data-lens-count="{{lensCount}}"><select></select></div>
                {{#trackers}}
                <section class="tracker" data-tracker="{{id}}">
                {{#metrics}}<div class="metric-card" data-stage="{{stage}}" style="--percent: {{percent}}"></div>{{/metrics}}
                <div class="chart"><canvas></canvas></div>
                </section>
                {{/trackers}}
                </div>
                {{{dashboardDataScript}}}
                </body></html>
                """);
        DashboardView view = twoTrackerView();

        File dashboardFile = writer.write(tempDir.toFile(), view, template.toFile());

        assertThat(validator.validate(dashboardFile)).isEmpty();
    }

    @Test
    @DisplayName("writeShouldRenderFromLensPackTemplateContentAndValidateItAgainstTheContract")
    void writeShouldRenderFromLensPackTemplateContentAndValidateItAgainstTheContract() throws IOException {
        String templateContent = """
                <!doctype html>
                <html><head><link rel="stylesheet" id="lens-stylesheet" href="{{defaultLensCssFile}}"></head>
                <body>
                <div class="dashboard">
                <h1>Venn Diagram View</h1>
                <div class="lens-switcher" data-lens-count="{{lensCount}}"><select></select></div>
                {{#trackers}}
                <section class="tracker" data-tracker="{{id}}">
                {{#metrics}}<div class="metric-card" data-stage="{{stage}}" style="--percent: {{percent}}"></div>{{/metrics}}
                <div class="chart"><canvas></canvas></div>
                </section>
                {{/trackers}}
                </div>
                {{{dashboardDataScript}}}
                </body></html>
                """;
        DashboardView view = twoTrackerView();

        File dashboardFile = writer.write(tempDir.toFile(), view, "venn-diagram-view", templateContent);

        String html = Files.readString(dashboardFile.toPath());
        assertThat(html).contains("Venn Diagram View");
        assertThat(validator.validate(dashboardFile)).isEmpty();
    }

    @Test
    @DisplayName("writeShouldFailWithAClearErrorWhenCustomTemplateHasInvalidMustacheSyntax")
    void writeShouldFailWithAClearErrorWhenCustomTemplateHasInvalidMustacheSyntax() throws IOException {
        Path template = tempDir.resolve("broken-template.html");
        Files.writeString(template, "<div>{{#trackers}}unclosed section</div>");
        DashboardView view = twoTrackerView();

        assertThatThrownBy(() -> writer.write(tempDir.toFile(), view, template.toFile()))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("broken-template.html");
    }

    @Test
    @DisplayName("writeShouldFailWithAClearErrorWhenCustomTemplateFileDoesNotExist")
    void writeShouldFailWithAClearErrorWhenCustomTemplateFileDoesNotExist() {
        File missingTemplate = tempDir.resolve("does-not-exist.html").toFile();
        DashboardView view = twoTrackerView();

        assertThatThrownBy(() -> writer.write(tempDir.toFile(), view, missingTemplate))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("does-not-exist.html");
    }

    private TrackerView trackerWithProjection(String trackerId, Confidence confidence) {
        Projection projection = new Projection(NOW.plus(Duration.ofDays(10)), 5, 10, 0.5, confidence);
        return new TrackerView(trackerId, List.of("listed"), List.of(new MetricCardView("listed", 5, 10, 50)), 10,
                Optional.of(projection), List.of(), Map.of(), List.of(), Map.of("listed", 5), List.of());
    }

    private DashboardView twoTrackerView() {
        TrackerViewFactory factory = new TrackerViewFactory();

        List<LifecycleRecord> gherkinRecords = List.of(
                new LifecycleRecord("g1", "Place an order", "Ordering",
                        Map.of("listed", NOW.minus(Duration.ofDays(20)), "defined", NOW.minus(Duration.ofDays(15)),
                                "implemented", NOW.minus(Duration.ofDays(10))),
                        NOW, null),
                new LifecycleRecord("g2", "Cancel an order", "Ordering",
                        Map.of("listed", NOW.minus(Duration.ofDays(20))), NOW, null));
        Projection gherkinProjection = new Projection(NOW.plus(Duration.ofDays(5)), 1, 2, 0.1, Confidence.LOW);
        TrackerView gherkinView = factory.build(
                "bdd-scenarios", List.of("listed", "defined", "implemented"), gherkinRecords,
                Optional.of(gherkinProjection), NOW, true);

        List<LifecycleRecord> apiRecords = List.of(
                new LifecycleRecord("a1", "GET /orders", "com.example.OrderController",
                        Map.of("declared", NOW.minus(Duration.ofDays(2))), NOW, null));
        TrackerView apiView = factory.build(
                "api-contracts", List.of("declared", "implemented", "verified"), apiRecords, Optional.empty(), NOW,
                false);

        List<ResolvedLens> lenses = List.of(
                new ResolvedLens("light-lens", "built-in", "body{background:#fff}".getBytes(StandardCharsets.UTF_8)),
                new ResolvedLens("dark-lens (midnight-theme)", "midnight-theme",
                        "body{background:#000}".getBytes(StandardCharsets.UTF_8)));

        return new DashboardView(List.of(gherkinView, apiView), lenses, "light-lens", "my-app Lens", "1.2.3");
    }
}
