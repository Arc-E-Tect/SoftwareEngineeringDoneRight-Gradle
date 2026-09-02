package com.arc_e_tect.gradle.architecture;

import com.arc_e_tect.gradle.dslupdater.DslExtensionSchema;
import com.arc_e_tect.gradle.dslupdater.DslPropertyKind;
import com.arc_e_tect.gradle.dslupdater.DslPropertySpec;

import java.util.List;

/**
 * The {@code architectureValidator {}} block's DSL property schema for
 * {@code updateArchitectureValidatorDSL} (see {@code com.arc_e_tect.gradle.dslupdater.DslUpdater}).
 *
 * <p>Unlike every other plugin in this repository, {@link ArchitectureValidatorExtension} is a
 * concrete class whose defaults are set in its own constructor (via {@code objects.xProperty()
 * .convention(...)}), not in {@code Plugin.apply()} - except {@code basePackage}, which the plugin
 * additionally overrides in {@link ArchitectureValidatorPlugin#apply} with a lazy provider derived
 * from {@code project.group}. Every top-level property has a real default.</p>
 *
 * <p>{@code junitVersion} is the one exception, deliberately left out: alongside the normal
 * {@code Property<String>} getter, {@link ArchitectureValidatorExtension} also exposes a plain
 * {@code setJunitVersion(String)} convenience setter, so a real build file may configure it either
 * as {@code junitVersion = '...'} or as {@code setJunitVersion('...')} - a project pulling the
 * version from a catalog (e.g. {@code setJunitVersion(libs.versions.junit.get())}) needs the method
 * form. {@code DslUpdater} only ever recognizes the {@code name = value} assignment form as "already
 * configured"; it would not recognize {@code setJunitVersion(...)} and would append a second,
 * conflicting {@code junitVersion = '6.1.0'} line - which, appended after the user's own line,
 * would execute second and silently override the real configured value. Found by testing this
 * schema against a real example that uses exactly this setter form.</p>
 *
 * <p>{@code hexagonalArchitecture} is a nested block ({@link HexagonalArchitectureExtension}), not
 * a {@code NamedDomainObjectContainer} like {@code trackerLens.trackers} - there's exactly one
 * instance, always present with its own real defaults regardless of whether the DSL text ever
 * mentions it. It's still modeled here as a {@link DslPropertyKind#CONTAINER}: {@code DslUpdater}
 * never adds to or otherwise touches a container property inside an <em>existing</em> outer block,
 * so if a project's {@code architectureValidator { }} block exists but has no
 * {@code hexagonalArchitecture { }} sub-block (or an incomplete one), this task leaves it alone -
 * a real limitation of not having true nested-block support yet, but a safe one: the nested
 * properties' real defaults already apply via their own {@code convention(...)} regardless of
 * whether the DSL text shows them, so an absent or partial sub-block is still behaviorally a
 * no-op. The container's stub is the real default values themselves (not a commented-out example
 * the way {@code trackerLens.trackers}'s is), so a freshly {@code --generateDSL}'d block is a
 * literal no-op too - except under {@code --cleanupDSL}, which skips the stub entirely (by
 * design, {@code DslUpdater} never adds a container's stub under cleanup), leaving an empty
 * {@code hexagonalArchitecture { }} block that is still behaviorally identical, just less
 * self-documenting.</p>
 */
final class ArchitectureValidatorDslSchema {

    private static final String HEXAGONAL_ARCHITECTURE_STUB = String.join("\n",
            "inPorts = ['..application.port.inbound..']",
            "outPorts = ['..application.port.outbound..']",
            "domainModel = ['..application.domain..']",
            "adapters = ['..adapter..', '..adapters..']",
            "inboundAdapters = ['..adapter.inbound..', '..adapters.inbound..']",
            "outboundAdapters = ['..adapter.outbound..', '..adapters.outbound..']",
            "applicationServices = ['..application.domain.service..', '..application.service..']",
            "commonPackages = ['..application.common..']",
            "namingConventionsEnabled = false");

    static final DslExtensionSchema SCHEMA = new DslExtensionSchema(ArchitectureValidatorExtension.NAME, List.of(
            DslPropertySpec.scalar("testDirectory", "layout.projectDirectory.dir('src/testArchitecture/java')",
                    "Directory holding hand-written architecture tests."),
            DslPropertySpec.scalar("basePackage",
                    "project.provider { project.group == null || project.group.toString() == 'unspecified' "
                            + "? '' : project.group.toString() }",
                    "Root package ArchUnit rules are scoped to."),
            DslPropertySpec.scalar("failOnViolation", "true",
                    "Whether the build fails when a rule is violated."),
            DslPropertySpec.scalar("maxAllowedViolations", "0",
                    "Violation count tolerated before failing."),
            DslPropertySpec.scalar("ignoreFailures", "false",
                    "Whether violations are reported but never fail the build."),
            DslPropertySpec.scalar("failOnDuplicateRules", "false",
                    "Whether the build fails when duplicate rules are discovered."),
            DslPropertySpec.scalar("useBuiltInHexagonalRulePack", "true",
                    "Whether the built-in hexagonal rule pack is generated."),
            DslPropertySpec.scalar("rulesDisabled", "[]",
                    "Rule names to skip."),
            DslPropertySpec.container("hexagonalArchitecture",
                    "Package-pattern overrides for the built-in hexagonal rule pack; always has real defaults of "
                            + "its own, whether or not this block is present.",
                    HEXAGONAL_ARCHITECTURE_STUB)
    ));

    private ArchitectureValidatorDslSchema() {
    }
}
