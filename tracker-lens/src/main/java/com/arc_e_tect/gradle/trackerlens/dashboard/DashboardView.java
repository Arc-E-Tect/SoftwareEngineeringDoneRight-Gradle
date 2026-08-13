package com.arc_e_tect.gradle.trackerlens.dashboard;

import com.arc_e_tect.gradle.trackerlens.lens.ResolvedLens;

import java.util.List;

/**
 * Everything {@link DashboardHtmlWriter} needs to render {@code dashboard.html} and one CSS file
 * per lens.
 *
 * @param trackers   the trackers to render, in registration order
 * @param lenses     every discovered lens, merged and auto-namespaced by
 *                   {@code com.arc_e_tect.gradle.trackerlens.lens.LensResolver}
 * @param defaultLensId the lens id active on first load
 */
public record DashboardView(List<TrackerView> trackers, List<ResolvedLens> lenses, String defaultLensId) {
}
