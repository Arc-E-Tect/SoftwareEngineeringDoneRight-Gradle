package com.arc_e_tect.gradle.trackerlens.lens;

import java.util.List;

/**
 * One contributor of {@link Template}s to be merged by {@link TemplateResolver}: either this
 * plugin's own bundled built-in template, or one resolved {@code lensStyle} dependency's templates.
 *
 * <p>Unlike {@link LensSource}, there is no {@code trackerLens.lensStylesheet}-equivalent
 * single-file contributor here - a template is only ever offered through a lens pack (built-in or
 * external), per {@code listTrackerLensTemplates}'s own scope; a project's own one-off template
 * still goes through the existing, unrelated {@code trackerLens.template} file property, entirely
 * outside this discovery mechanism.</p>
 *
 * @param label     identifies this contributor for the auto-namespacing suffix applied to a
 *                  colliding template id
 * @param templates the templates this contributor provides
 */
public record TemplateSource(String label, List<Template> templates) {
}
