package com.arc_e_tect.gradle.trackerlens.lens;

/**
 * A {@link Template} after cross-pack merging by {@link TemplateResolver}: its final, possibly
 * auto-namespaced id, the label of the source it came from, and its Mustache content.
 *
 * @param id          the final template id offered by {@code listTrackerLensTemplates} and
 *                    selectable via {@code trackerLens.templateId}
 * @param sourceLabel the label of the {@link TemplateSource} this template was resolved from
 * @param content     the raw Mustache template file bytes
 */
public record ResolvedTemplate(String id, String sourceLabel, byte[] content) {
}
