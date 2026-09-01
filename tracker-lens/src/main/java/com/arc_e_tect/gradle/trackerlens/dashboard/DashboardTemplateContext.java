package com.arc_e_tect.gradle.trackerlens.dashboard;

import com.arc_e_tect.gradle.trackerlens.lens.LensNaming;
import com.arc_e_tect.gradle.trackerlens.lens.ResolvedLens;
import com.arc_e_tect.gradle.trackerlens.projection.Confidence;
import com.arc_e_tect.gradle.trackerlens.projection.Projection;
import com.arc_e_tect.gradle.trackerlens.tracker.LifecycleRecord;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the Mustache context {@link DashboardHtmlWriter} renders {@code dashboard.html.mustache}
 * (or a user-supplied override) against.
 *
 * <p>Every value placed here is either genuinely data-bound (a tracker's id, a metric's count, a
 * projection's date) or one of the two fixed raw-HTML blocks ({@code dashboardDataScript},
 * {@code bootstrapScript}) - never a piece of the dashboard's static wording. A template's own
 * literal text (headings, captions, column labels) is not represented as context variables at all:
 * a translating template author edits that text directly in their copy of the template, the same
 * way they would in any other Mustache template.</p>
 */
final class DashboardTemplateContext {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private DashboardTemplateContext() {}

    static Map<String, Object> build(DashboardView view) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("dashboardName", view.dashboardName());
        context.put("version", view.version());
        context.put("defaultLensCssFile", LensNaming.cssFileName(view.defaultLensId()));
        context.put("lensCount", view.lenses().size());
        context.put("lenses", view.lenses().stream().map(lens -> lensContext(lens, view.defaultLensId())).toList());
        context.put("trackers", view.trackers().stream().map(DashboardTemplateContext::trackerContext).toList());

        List<Map<String, Object>> staleItems = staleItemsContext(view.trackers());
        context.put("staleItems", staleItems);
        context.put("hasStaleItems", !staleItems.isEmpty());

        context.put("dashboardDataScript",
                "<script type=\"application/json\" id=\"dashboard-data\">" + DashboardJson.write(view) + "</script>");
        context.put("bootstrapScript", DashboardBootstrapScript.SOURCE);
        return context;
    }

    private static Map<String, Object> lensContext(ResolvedLens lens, String defaultLensId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", lens.id());
        map.put("selected", lens.id().equals(defaultLensId));
        return map;
    }

    private static Map<String, Object> trackerContext(TrackerView tracker) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", tracker.id());
        map.put("metrics", tracker.metrics().stream().map(DashboardTemplateContext::metricContext).toList());
        tracker.projection().ifPresent(projection -> map.put("projection", projectionContext(projection)));
        return map;
    }

    private static Map<String, Object> metricContext(MetricCardView metric) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("stage", metric.stage());
        map.put("count", metric.count());
        map.put("totalCount", metric.totalCount());
        map.put("percent", metric.percent());
        return map;
    }

    private static Map<String, Object> projectionContext(Projection projection) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", projection.confidence().name().toLowerCase(Locale.ROOT));
        map.put("projectedDate", DATE_FORMATTER.format(projection.projectedDate()));
        map.put("currentCount", projection.currentCount());
        map.put("totalCount", projection.totalCount());
        map.put("velocityPerDay", String.format(Locale.ROOT, "%.2f", projection.velocityPerDay()));
        map.put("disclaimer", disclaimerFor(projection.confidence()));
        return map;
    }

    private static String disclaimerFor(Confidence confidence) {
        return switch (confidence) {
            case LOW -> "Early estimate, based on a short window of recent history - "
                    + "this will get more accurate as more history is captured.";
            case MEDIUM -> "Based on a growing window of history; accuracy is improving.";
            case HIGH -> "Based on a full 90-day window of history.";
        };
    }

    private static List<Map<String, Object>> staleItemsContext(List<TrackerView> trackers) {
        return trackers.stream()
                .flatMap(tracker -> tracker.staleItems().stream().map(record -> staleItemContext(tracker.id(), record)))
                .toList();
    }

    private static Map<String, Object> staleItemContext(String trackerId, LifecycleRecord record) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("trackerId", trackerId);
        map.put("label", record.label());
        map.put("group", record.group() == null ? "" : record.group());
        map.put("lastSeen", lastSeenText(record));
        return map;
    }

    private static String lastSeenText(LifecycleRecord record) {
        Instant lastSeenAt = record.lastSeenAt();
        return lastSeenAt == null ? "unknown" : DATE_FORMATTER.format(lastSeenAt);
    }
}
