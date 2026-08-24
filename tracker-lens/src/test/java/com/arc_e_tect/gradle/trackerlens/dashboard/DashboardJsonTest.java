package com.arc_e_tect.gradle.trackerlens.dashboard;

import com.arc_e_tect.gradle.trackerlens.lens.ResolvedLens;
import com.arc_e_tect.gradle.trackerlens.tracker.ResponseCoverageCell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DashboardJson")
class DashboardJsonTest {

    @Test
    @DisplayName("writeShouldEmbedTrackerSeriesAndLensFileMapping")
    void writeShouldEmbedTrackerSeriesAndLensFileMapping() {
        Map<String, Integer> stageBreakdown = new LinkedHashMap<>();
        stageBreakdown.put("listed", 1);
        stageBreakdown.put("defined", 2);
        TrackerView tracker = new TrackerView(
                "bdd-scenarios", List.of("listed", "defined"), List.of(), 0, Optional.empty(),
                List.of(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2)),
                Map.of("listed", List.of(1, 2), "defined", List.of(0, 1)),
                List.of(), stageBreakdown, List.of(stageBreakdown, stageBreakdown));
        DashboardView view = new DashboardView(
                List.of(tracker),
                List.of(new ResolvedLens("dark-lens (midnight-theme)", "midnight-theme", new byte[0])),
                "dark-lens (midnight-theme)", "my-app Lens", "1.2.3");

        String json = DashboardJson.write(view);

        assertThat(json).contains("\"id\":\"bdd-scenarios\"", "\"2026-08-01\"", "\"2026-08-02\"",
                "\"dark-lens (midnight-theme)\":\"dark-lens-midnight-theme.css\"",
                "\"stageBreakdown\":{\"listed\":1,\"defined\":2}");
    }

    @Test
    @DisplayName("writeShouldEmbedSeriesByGroupAndMatrix")
    void writeShouldEmbedSeriesByGroupAndMatrix() {
        Map<String, Integer> stageBreakdown = new LinkedHashMap<>();
        stageBreakdown.put("declared", 1);
        stageBreakdown.put("covered", 1);
        Map<String, List<Integer>> twoXxSeries = new LinkedHashMap<>();
        twoXxSeries.put("declared", List.of(1, 1));
        twoXxSeries.put("covered", List.of(0, 1));
        Map<String, Map<String, List<Integer>>> seriesByGroup = new LinkedHashMap<>();
        seriesByGroup.put("2xx", twoXxSeries);
        TrackerView tracker = new TrackerView(
                "response-coverage", List.of("declared", "covered"), List.of(), 0, Optional.empty(),
                List.of(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2)),
                Map.of("declared", List.of(1, 1), "covered", List.of(0, 1)),
                List.of(), stageBreakdown, List.of(stageBreakdown, stageBreakdown),
                seriesByGroup, List.of(new ResponseCoverageCell("GET", "/v1/foobars", "200", 2, true)));
        DashboardView view = new DashboardView(
                List.of(tracker),
                List.of(new ResolvedLens("light-lens", null, new byte[0])),
                "light-lens", "my-app Lens", "1.2.3");

        String json = DashboardJson.write(view);

        assertThat(json).contains(
                "\"seriesByGroup\":{\"2xx\":{\"declared\":[1,1],\"covered\":[0,1]}}",
                "\"matrix\":[{\"verb\":\"GET\",\"path\":\"/v1/foobars\",\"responseCode\":\"200\","
                        + "\"testCount\":2,\"covered\":true}]");
    }

    @Test
    @DisplayName("writeShouldEmbedEmptySeriesByGroupAndMatrixWhenAbsent")
    void writeShouldEmbedEmptySeriesByGroupAndMatrixWhenAbsent() {
        Map<String, Integer> stageBreakdown = new LinkedHashMap<>();
        stageBreakdown.put("listed", 1);
        TrackerView tracker = new TrackerView(
                "bdd-scenarios", List.of("listed"), List.of(), 0, Optional.empty(),
                List.of(LocalDate.of(2026, 8, 1)), Map.of("listed", List.of(1)),
                List.of(), stageBreakdown, List.of(stageBreakdown));
        DashboardView view = new DashboardView(
                List.of(tracker),
                List.of(new ResolvedLens("light-lens", null, new byte[0])),
                "light-lens", "my-app Lens", "1.2.3");

        String json = DashboardJson.write(view);

        assertThat(json).contains("\"seriesByGroup\":{}", "\"matrix\":[]");
    }
}
