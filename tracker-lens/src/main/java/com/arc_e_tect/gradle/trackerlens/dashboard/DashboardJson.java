package com.arc_e_tect.gradle.trackerlens.dashboard;

import com.arc_e_tect.gradle.trackerlens.lens.LensNaming;
import com.arc_e_tect.gradle.trackerlens.lens.ResolvedLens;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serializes the {@code #dashboard-data} JSON blob embedded in {@code dashboard.html}: chart series
 * for the fixed bootstrap script to feed to Chart.js, and the lens-id-to-file-name mapping for the
 * lens switcher. Hand-rolled, no JSON library dependency, since the shape is small and fixed.
 */
final class DashboardJson {

    private DashboardJson() {}

    static String write(DashboardView view) {
        StringBuilder json = new StringBuilder();
        json.append("{\"trackers\":[");
        for (int i = 0; i < view.trackers().size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            writeTracker(json, view.trackers().get(i));
        }
        json.append("],\"lensFiles\":{");
        List<ResolvedLens> lenses = view.lenses();
        for (int i = 0; i < lenses.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(quote(lenses.get(i).id())).append(':').append(quote(LensNaming.cssFileName(lenses.get(i).id())));
        }
        json.append("}}");
        return json.toString();
    }

    private static void writeTracker(StringBuilder json, TrackerView tracker) {
        json.append("{\"id\":").append(quote(tracker.id()));
        json.append(",\"stages\":[")
                .append(tracker.stages().stream().map(DashboardJson::quote).collect(Collectors.joining(",")))
                .append(']');
        json.append(",\"dates\":[")
                .append(tracker.chartDates().stream().map(LocalDate::toString).map(DashboardJson::quote)
                        .collect(Collectors.joining(",")))
                .append(']');
        json.append(",\"series\":{");
        boolean first = true;
        for (Map.Entry<String, List<Integer>> entry : tracker.chartSeries().entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append(quote(entry.getKey())).append(":[")
                    .append(entry.getValue().stream().map(String::valueOf).collect(Collectors.joining(",")))
                    .append(']');
        }
        json.append("},\"stageBreakdown\":{");
        first = true;
        for (Map.Entry<String, Integer> entry : tracker.stageBreakdown().entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append(quote(entry.getKey())).append(':').append(entry.getValue());
        }
        json.append("}}");
    }

    private static String quote(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 2);
        escaped.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(c);
            }
        }
        escaped.append('"');
        return escaped.toString();
    }
}
