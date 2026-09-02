package com.arc_e_tect.gradle.gherkin;

import com.arc_e_tect.gradle.dslupdater.DslExtensionSchema;
import com.arc_e_tect.gradle.dslupdater.DslPropertySpec;

import java.util.List;

/**
 * The {@code gherkinToAsciidoc {}} block's DSL property schema for
 * {@code updateGherkinToAsciidocDSL} (see {@code com.arc_e_tect.gradle.dslupdater.DslUpdater}).
 *
 * <p>This schema is deliberately much smaller than the extension's full property list, because
 * {@link GherkinToAsciidocPlugin#apply} has a root-project-inheritance fork: in a sub-project whose
 * root project also applies this plugin, {@code trackProgress}, {@code outputFileName},
 * {@code template}, {@code systemUnderTestVersion}, {@code indexing}, {@code forceRewrite},
 * {@code consolidatedIndex}, and {@code trackProgressHistory} all default to the <em>root</em>
 * project's own configured value instead of a fixed literal - a {@code Property#convention(Provider)}
 * link that only holds as long as the property is never explicitly {@code set(...)}. Writing any of
 * those eight as a literal into a sub-project's build file would permanently break that inheritance
 * - exactly the opposite of a no-op - the moment the root project's own value differs (now or
 * later) from whatever literal was written. {@code includeSubDirs} and {@code groupByFeature} are
 * excluded for the same reason, compounded further by also depending on {@code trackProgress}
 * itself. This module has no way to know, from a single build file's text, whether it belongs to a
 * root or a sub-project, so none of these ten can be included safely.
 *
 * <p>{@code sourceDirs}, {@code sourceFile}, {@code glueCodeDirs}, and {@code template} are also
 * excluded: the first three are never given an extension-level default at all ({@code sourceDirs}
 * falls back to a hardcoded path only inside the task's own execution, not via
 * {@code convention(...)}; the other two are genuinely optional). {@code template} has no default
 * on a root/standalone project either.
 *
 * <p>Only {@code outputDir}, {@code snippetDir}, {@code progressHistoryFile}, and
 * {@code updateProgressHistory} are always given the same value regardless of root/sub-project
 * status - {@link GherkinToAsciidocPlugin#apply}'s own comment notes they deliberately always
 * default to <em>this</em> project's own directory, never the root's, so every project's
 * report/history lands in its own location by default. Each literal below is the same value that
 * property's own {@code convention(...)} call resolves to, so that writing it out explicitly is
 * always a no-op.</p>
 */
final class GherkinToAsciidocDslSchema {

    static final DslExtensionSchema SCHEMA = new DslExtensionSchema(GherkinToAsciidocExtension.NAME, List.of(
            DslPropertySpec.scalar("outputDir", "layout.buildDirectory.dir('generated-docs')",
                    "Where the generated AsciiDoc is written."),
            DslPropertySpec.scalar("snippetDir",
                    "layout.buildDirectory.dir('" + GherkinToAsciidocExtension.DEFAULT_SNIPPET_DIR + "')",
                    "Where report snippets are written."),
            DslPropertySpec.scalar("progressHistoryFile",
                    "layout.projectDirectory.file('"
                            + GherkinToAsciidocExtension.DEFAULT_PROGRESS_HISTORY_FILE_NAME + "')",
                    "History file the scenario progress history is read from and written back to (project "
                            + "directory, meant to be committed)."),
            DslPropertySpec.scalar("updateProgressHistory", "trackProgressHistory",
                    "Whether the history file is written back to disk; tracks trackProgressHistory live rather "
                            + "than a fixed value.")
    ));

    private GherkinToAsciidocDslSchema() {
    }
}
